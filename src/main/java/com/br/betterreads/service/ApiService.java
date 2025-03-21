package com.br.betterreads.service;

import com.br.betterreads.model.OpenLibraryApi;
import com.br.betterreads.model.OpenLibraryTrendingResponse;
import com.br.betterreads.util.BookMapper;
import com.br.betterreads.model.Book;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ApiService {

    private final RestTemplate restTemplate;
    private final BookMapper bookMapper;

    public ApiService(RestTemplate restTemplate, BookMapper bookMapper) {
        this.restTemplate = restTemplate;
        this.bookMapper = bookMapper;
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
                String[] parts = url.split("/");
                String bookId = "";
                for (int i = 0; i < parts.length; i++) {
                    if ("books".equals(parts[i]) && i + 1 < parts.length) {
                        bookId = parts[i + 1];
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
                            Map<String, Object> firstWork = (Map<String, Object>) worksList.getFirst();
                            Object keyObj = firstWork.get("key");
                            if (keyObj instanceof String workKey) {
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
                                    Object descObj = workDetails.get("description");
                                    String description = getDescription(descObj);
                                    book = bookMapper.updateBookWithDescription(book, description);
                                }

                                if (workDetails != null && workDetails.containsKey("subtitle")) {
                                    Object subtitleObj = workDetails.get("subtitle");
                                    String subtitle = getSubtitle(subtitleObj);
                                    book.setSubtitle(subtitle);
                                }
                            }
                        }
                    }
                }
            }

            return book;
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
            Map<String, Object> descMap = (Map<String, Object>) descObj;
            Object valueObj = descMap.get("value");
            return valueObj instanceof String ? (String) valueObj : "No description available";
        }
        return "No description available";
    }

    private static String getSubtitle(Object subtitleObj) {
        if (subtitleObj instanceof String) {
            return (String) subtitleObj;
        } else if (subtitleObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> subtitleMap = (Map<String, Object>) subtitleObj;
            Object valueObj = subtitleMap.get("value");
            return valueObj instanceof String ? (String) valueObj : null;
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
