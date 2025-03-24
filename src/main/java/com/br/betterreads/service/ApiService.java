package com.br.betterreads.service;

import com.br.betterreads.model.OpenLibraryApi;
import com.br.betterreads.model.OpenLibraryTrendingResponse;
import com.br.betterreads.util.BookMapper;
import com.br.betterreads.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiService {

    private final RestTemplate restTemplate;
    private final BookMapper bookMapper;

    public ApiService(RestTemplate restTemplate, BookMapper bookMapper) {
        this.restTemplate = restTemplate;
        this.bookMapper = bookMapper;
    }

    /**
     * Truncates a string to a maximum length safely
     *
     * @param value String to truncate
     * @return Truncated string or original if null or shorter
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * Sanitizes all string fields in a Book entity to ensure they don't exceed
     * the database column size limits
     * @param book Book to sanitize
     * @return Sanitized book
     */
    private Book sanitizeBookFields(Book book) {
        if (book == null) return null;

        book.setTitle(truncate(book.getTitle(), 255));
        book.setSubtitle(truncate(book.getSubtitle(), 255));
        book.setAuthor(truncate(book.getAuthor(), 255));
        book.setDescription(truncate(book.getDescription(), 1000));
        book.setCoverURL(truncate(book.getCoverURL(), 255));
        book.setIsbn(truncate(book.getIsbn(), 13));

        if (book.getGenre() != null) {
            String[] sanitizedGenre = new String[book.getGenre().length];
            for (int i = 0; i < book.getGenre().length; i++) {
                sanitizedGenre[i] = truncate(book.getGenre()[i], 255);
            }
            book.setGenre(sanitizedGenre);
        }

        return book;
    }

    private Map<String, Object> performSearchRequest(String url) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error during API request: " + e.getMessage());
            return null;
        }
    }

    public List<Book> searchBookByAuthor(String author) {
        try {
            String searchUrl = String.format(
                    "https://openlibrary.org/search.json?author=%s&limit=100",
                    URLEncoder.encode(author, StandardCharsets.UTF_8)
            );

            Map<String, Object> result = performSearchRequest(searchUrl);
            if (result != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.valueToTree(result);
                List<Book> books = extractBooksFromSearchResult(jsonNode);
                return books.stream()
                        .map(this::sanitizeBookFields)
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error searching books by author: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Book> searchBookByTitle(String title) {
        try {
            String searchUrl = String.format(
                    "https://openlibrary.org/search.json?title=%s&limit=100",
                    URLEncoder.encode(title, StandardCharsets.UTF_8)
            );

            Map<String, Object> result = performSearchRequest(searchUrl);
            if (result != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.valueToTree(result);
                List<Book> books = extractBooksFromSearchResult(jsonNode);
                return books.stream()
                        .map(this::sanitizeBookFields)
                        .collect(Collectors.toList());
            }
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error searching books by title: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<Book> extractBooksFromSearchResult(JsonNode root) {
        List<Book> books = new ArrayList<>();
        Set<String> uniqueKeys = new HashSet<>();

        if (root != null && root.has("docs")) {
            JsonNode docs = root.get("docs");
            for (JsonNode doc : docs) {
                String key = doc.has("key") ? doc.get("key").asText() : null;
                if (key == null || uniqueKeys.contains(key)) {
                    continue;
                }
                uniqueKeys.add(key);

                Book book = new Book();

                if (doc.has("title")) {
                    book.setTitle(doc.get("title").asText());
                } else {
                    book.setTitle("Unknown Title");
                }
                book.setSubtitle("");

                if (doc.has("author_name") && doc.get("author_name").isArray()) {
                    StringBuilder authorBuilder = new StringBuilder();
                    JsonNode authorNodes = doc.get("author_name");
                    for (int i = 0; i < authorNodes.size(); i++) {
                        if (i > 0) {
                            authorBuilder.append(", ");
                        }
                        authorBuilder.append(authorNodes.get(i).asText());
                    }
                    book.setAuthor(authorBuilder.toString());
                } else {
                    book.setAuthor("Unknown Author");
                }

                String isbn = null;
                if (doc.has("isbn") && doc.get("isbn").isArray() && !doc.get("isbn").isEmpty()) {
                    isbn = doc.get("isbn").get(0).asText();
                    isbn = normalizeIsbn(isbn);
                } else {
                    isbn = generateIsbnFromKey(key);
                }
                book.setIsbn(isbn);

                if (doc.has("cover_i")) {
                    book.setCoverURL("https://covers.openlibrary.org/b/id/" + doc.get("cover_i").asText() + "-M.jpg");
                } else {
                    book.setCoverURL("/images/covertemplate.jpg");
                }

                if (doc.has("first_publish_year")) {
                    book.setPublicationYear(doc.get("first_publish_year").asInt());
                }
                book.setApiId(generateApiIdFromKey(key));
                book.setLastSync(LocalDateTime.now());
                books.add(book);
            }
        }
        return books;
    }

    private String normalizeIsbn(String isbn) {
        if (isbn == null) return "0000000000000";
        String cleaned = isbn.replaceAll("[^0-9X]", "");
        if (cleaned.length() > 13) {
            return cleaned.substring(0, 13);
        } else if (cleaned.length() < 13) {
            return cleaned + "0".repeat(13 - cleaned.length());
        }
        return cleaned;
    }

    private String generateIsbnFromKey(String key) {
        if (key == null) return "0000000000000";
        String cleanKey = key.replaceAll("[^a-zA-Z0-9]", "");
        int hashCode = Math.abs(cleanKey.hashCode());
        String isbn = String.format("%013d", hashCode % 10000000000000L);
        if (isbn.length() > 13) {
            return isbn.substring(0, 13);
        }
        return isbn;
    }

    private int generateApiIdFromKey(String key) {
        if (key == null) return new Random().nextInt(1000000);

        try {
            return Math.abs(key.hashCode() % 1000000000);
        } catch (Exception e) {
            return new Random().nextInt(1000000);
        }
    }


    public Book fetchBookFromApi(String isbn) {
        try {
            String isbnUrl = String.format(
                    "https://openlibrary.org/api/books?bibkeys=ISBN:%s&format=json&jscmd=data",
                    isbn
            );

            ResponseEntity<Map<String, OpenLibraryApi>> response = restTemplate.exchange(
                    isbnUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            OpenLibraryApi bookData = Objects.requireNonNull(response.getBody()).get("ISBN:" + isbn);
            if (bookData == null) {
                return null;
            }
            Book book = bookMapper.convertToBook(bookData, isbn);
            String url = bookData.getUrl();
            if (url != null) {
                String[] split = url.split("/");
                String bookId = "";
                for (int i = 0; i < split.length; i++) {
                    if ("books".equals(split[i]) && i + 1 < split.length) {
                        bookId = split[i + 1];
                        break;
                    }
                }

                String bookDetailsUrl = "https://openlibrary.org/books/" + bookId + ".json";
                ResponseEntity<Map<String, Object>> bookDetailsResponse = restTemplate.exchange(
                        bookDetailsUrl,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

                Map<String, Object> bookDetails = bookDetailsResponse.getBody();
                if (bookDetails != null && bookDetails.containsKey("works")) {
                    Object worksObj = bookDetails.get("works");
                    if (worksObj instanceof List<?> worksList) {
                        if (!worksList.isEmpty() && worksList.getFirst() instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> first = (Map<String, Object>) worksList.getFirst();
                            Object key = first.get("key");
                            if (key instanceof String workKey) {
                                String workId = workKey.replace("/works/", "");
                                String workUrl = "https://openlibrary.org/works/" + workId + ".json";
                                ResponseEntity<Map<String, Object>> workDetailsResponse = restTemplate.exchange(
                                        workUrl,
                                        HttpMethod.GET,
                                        null,
                                        new ParameterizedTypeReference<>() {}
                                );

                                Map<String, Object> workDetails = workDetailsResponse.getBody();
                                if (workDetails != null && workDetails.containsKey("description")) {
                                    Object desObj = workDetails.get("description");
                                    String description = getDescription(desObj);
                                    book = bookMapper.updateBookWithDescription(book, description);
                                }

                                if (workDetails != null && workDetails.containsKey("subtitle")) {
                                    Object subObj = workDetails.get("subtitle");
                                    String subtitle = getSubtitle(subObj);
                                    book.setSubtitle(subtitle);
                                }
                            }
                        }
                    }
                }
            }
            if (book.getCoverURL() == null) {
                book.setCoverURL("/images/covertemplate.jpg");
            }
            if (book.getSubtitle() == null) {
                book.setSubtitle("");
            }
            return sanitizeBookFields(book);
        } catch (Exception e) {
            System.err.println("Error fetching book data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static String getDescription(Object descObj) {
        if (descObj instanceof String) {
            return (String) descObj;
        } else if (descObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> DesMap = (Map<String, Object>) descObj;
            Object ValObj = DesMap.get("value");
            return ValObj instanceof String ? (String) ValObj : "No description available";
        }
        return "No description available";
    }

    private static String getSubtitle(Object subtitleObj) {
        if (subtitleObj instanceof String) {
            return (String) subtitleObj;
        } else if (subtitleObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> SubMap = (Map<String, Object>) subtitleObj;
            Object ValObj = SubMap.get("value");
            return ValObj instanceof String ? (String) ValObj : null;
        }
        return null;
    }


    public List<Book> fetchTrendingBooks(int limit) {
        String apiUrl = "https://openlibrary.org/trending/weekly.json?limit=" + limit;

        ResponseEntity<OpenLibraryTrendingResponse> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                OpenLibraryTrendingResponse.class
        );

        if(response.getBody() == null || response.getBody().getWorks() == null) {
            return List.of();
        }

        return response.getBody().getWorks().stream()
                .map(bookMapper::convertTrendingBook)
                .toList();
    }
}
