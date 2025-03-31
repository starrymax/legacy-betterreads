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
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class ApiService {

    private final RestTemplate restTemplate;
    private final BookMapper bookMapper;

    private final Map<String, List<Book>> searchResultsCache = new ConcurrentHashMap<>();
    private final Map<String, Long> searchTimestamps = new ConcurrentHashMap<>();
    private static final long SEARCH_CACHE_TTL = 1800000;

    public ApiService(RestTemplate restTemplate, BookMapper bookMapper) {
        this.restTemplate = restTemplate;
        this.bookMapper = bookMapper;
    }


    /**
     * Fetches ISBN from work editions
     *
     * @param workKey Work key (e.g., "/works/OL27482W")
     * @return Valid ISBN or fallback
     */
    private final Map<String, Map<String, Object>> workDetailsCache = new HashMap<>();

    private String fetchIsbnFromWork(String workKey) {
        if (workKey == null) return "0000000000000";

        try {
            Map<String, Object> workDetails = workDetailsCache.get(workKey);
            if (workDetails != null) {
                String isbn = extractIsbnFromWorkDetails(workDetails);
                if (isbn != null) return isbn;
            }

            String editionsUrl = "https://openlibrary.org" + workKey + "/editions.json?limit=10&fields=isbn_10,isbn_13";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    editionsUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> editions = response.getBody();
            if (editions != null && editions.containsKey("entries")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> entries = (List<Map<String, Object>>) editions.get("entries");

                for (Map<String, Object> entry : entries) {
                    if (entry.containsKey("isbn_13")) {
                        @SuppressWarnings("unchecked")
                        List<String> isbn13List = (List<String>) entry.get("isbn_13");
                        if (isbn13List != null && !isbn13List.isEmpty()) {
                            return normalizeIsbn(isbn13List.getFirst());
                        }
                    }
                }

                for (Map<String, Object> entry : entries) {
                    if (entry.containsKey("isbn_10")) {
                        @SuppressWarnings("unchecked")
                        List<String> isbn10List = (List<String>) entry.get("isbn_10");
                        if (isbn10List != null && !isbn10List.isEmpty()) {
                            return normalizeIsbn(isbn10List.getFirst());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching editions: " + e.getMessage());
        }

        return generateIsbnFromKey(workKey);
    }

    private String extractIsbnFromWorkDetails(Map<String, Object> workDetails) {
        if (workDetails.containsKey("identifiers")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> identifiers = (Map<String, Object>) workDetails.get("identifiers");

            if (identifiers.containsKey("isbn_13")) {
                @SuppressWarnings("unchecked")
                List<String> isbn13 = (List<String>) identifiers.get("isbn_13");
                if (isbn13 != null && !isbn13.isEmpty()) {
                    return normalizeIsbn(isbn13.getFirst());
                }
            }

            if (identifiers.containsKey("isbn_10")) {
                @SuppressWarnings("unchecked")
                List<String> isbn10 = (List<String>) identifiers.get("isbn_10");
                if (isbn10 != null && !isbn10.isEmpty()) {
                    return normalizeIsbn(isbn10.getFirst());
                }
            }
        }
        return null;
    }

    /**
     * Fetches work details from Open Library API
     *
     * @param workKey The work key from Open Library (should include the leading slash)
     * @return Map containing work details or null if not found/error
     */
    private Map<String, Object> fetchWorkDetails(String workKey) {
        if (workKey == null) return null;


        if (!workKey.startsWith("/")) {
            workKey = "/" + workKey;
        }

        if (!workKey.startsWith("/works/") && !workKey.contains("/works/")) {
            workKey = "/works" + workKey;
        }

        if (workDetailsCache.containsKey(workKey)) {
            return workDetailsCache.get(workKey);
        }

        String workUrl = "https://openlibrary.org" + workKey + ".json";
        try {
            ResponseEntity<Map<String, Object>> workResponse = restTemplate.exchange(
                    workUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            Map<String, Object> details = workResponse.getBody();
            if (details != null) {
                workDetailsCache.put(workKey, details);
            }
            return details;
        } catch (Exception e) {
            System.err.println("Error fetching work details: " + e.getMessage());
            return null;
        }
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

    public List<Book> searchBookByTitle(String title) {
        String cacheKey = "title:" + title.toLowerCase();
        return getCachedOrFetchResults(cacheKey, () -> {
            try {
                String searchUrl = String.format(
                        "https://openlibrary.org/search.json?title=%s&limit=100&fields=*,author_name,cover_i,isbn,first_publish_year,description,subject,works",
                        URLEncoder.encode(title, StandardCharsets.UTF_8)
                );
                return processSearchRequest(searchUrl);
            } catch (Exception e) {
                System.err.println("Error searching books by title: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    public List<Book> searchBookByAuthor(String author) {
        String cacheKey = "author:" + author.toLowerCase();
        return getCachedOrFetchResults(cacheKey, () -> {
            try {
                String searchUrl = String.format(
                        "https://openlibrary.org/search.json?author=%s&limit=100&fields=*,author_name,cover_i,isbn,first_publish_year,description,subject,works",
                        URLEncoder.encode(author, StandardCharsets.UTF_8)
                );
                return processSearchRequest(searchUrl);
            } catch (Exception e) {
                System.err.println("Error searching books by author: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    private List<Book> getCachedOrFetchResults(String cacheKey, Supplier<List<Book>> dataFetcher) {
        if (searchResultsCache.containsKey(cacheKey)) {
            long timestamp = searchTimestamps.getOrDefault(cacheKey, 0L);
            if (System.currentTimeMillis() - timestamp < SEARCH_CACHE_TTL) {
                return new ArrayList<>(searchResultsCache.get(cacheKey));
            }
        }

        List<Book> results = dataFetcher.get();

        searchResultsCache.put(cacheKey, new ArrayList<>(results));
        searchTimestamps.put(cacheKey, System.currentTimeMillis());

        return results;
    }

    private List<Book> extractBooksFromSearchResult(JsonNode root) {
        Map<String, Book> uniqueBooks = new HashMap<>();
        List<Book> resultBooks = new ArrayList<>();


        if (root != null && root.has("docs")) {
            JsonNode docs = root.get("docs");

            for (JsonNode doc : docs) {
                Book book = new Book();

                String title = doc.has("title") ? doc.get("title").asText() : "Unknown Title";
                book.setTitle(title);
                book.setSubtitle("");

                String author = "Unknown Author";
                if (doc.has("author_name") && doc.get("author_name").isArray()) {
                    StringBuilder authorBuilder = new StringBuilder();
                    JsonNode authorNodes = doc.get("author_name");
                    for (int i = 0; i < authorNodes.size(); i++) {
                        if (i > 0) authorBuilder.append(", ");
                        authorBuilder.append(authorNodes.get(i).asText());
                    }
                    author = authorBuilder.toString();
                }
                book.setAuthor(author);

                String compositeKey = title.toLowerCase() + "||" + author.toLowerCase();
                String key = doc.has("key") ? doc.get("key").asText() : null;
                String workKey = null;

                if (key != null) {
                    if (key.startsWith("/works/")) {
                        workKey = key;
                    } else if (doc.has("work_key")) {
                        workKey = doc.get("work_key").asText();
                    } else if (key.startsWith("/books/")) {
                        try {
                            workKey = extractWorkKeyFromBook(key);
                        } catch (Exception e) {
                            System.err.println("Error fetching book details: " + e.getMessage());
                        }
                    }
                }
                book.setDescription("No description available");

                // Set cover URL
                if (doc.has("cover_i")) {
                    book.setCoverURL("https://covers.openlibrary.org/b/id/" + doc.get("cover_i").asText() + "-M.jpg");
                } else {
                    book.setCoverURL("/images/template.avif");
                }

                // Set publication year
                if (doc.has("first_publish_year")) {
                    book.setPublicationYear(doc.get("first_publish_year").asInt());
                }

                String isbn;
                if (doc.has("isbn") && doc.get("isbn").isArray() && !doc.get("isbn").isEmpty()) {
                    isbn = normalizeIsbn(doc.get("isbn").get(0).asText());
                } else {
                    isbn = workKey != null ? fetchIsbnFromWork(workKey) : "0000000000000";
                }

                if (isbn.equals("0000000000000")) {
                    isbn = generateIsbnFromKey(workKey != null ? workKey : key);
                }

                book.setIsbn(isbn);

                if (doc.has("subject") && doc.get("subject").isArray()) {
                    setGenresFromSubjects(doc.get("subject"), book);
                }

                if (workKey != null) {
                    enhanceBookWithWorkDetails(book, workKey);
                }

                book.setApiId(generateApiIdFromKey(key));
                book.setLastSync(LocalDateTime.now());

                boolean shouldReplace;
                if (!uniqueBooks.containsKey(compositeKey)) {
                    shouldReplace = true;
                } else {

                    Book existingBook = uniqueBooks.get(compositeKey);
                    shouldReplace = isBookBetterQuality(book, existingBook);
                }

                if (shouldReplace) {
                    uniqueBooks.put(compositeKey, book);
                }

                resultBooks.add(book);
            }
        }

        resultBooks.sort((b1, b2) -> {
            int score1 = calculateBookQualityScore(b1);
            int score2 = calculateBookQualityScore(b2);

            if (score1 != score2) {
                return score2 - score1;
            }

            String title1 = b1.getTitle().toLowerCase();
            String title2 = b2.getTitle().toLowerCase();

            if (title1.equals(title2)) {
                return 0;
            }

            assert root != null;
            String searchQuery = root.has("q") ? root.get("q").asText().toLowerCase() : "";

            boolean exactMatch1 = title1.equals(searchQuery);
            boolean exactMatch2 = title2.equals(searchQuery);

            if (exactMatch1) return -1;
            if (exactMatch2) return 1;

            boolean startsWith1 = title1.startsWith(searchQuery);
            boolean startsWith2 = title2.startsWith(searchQuery);

            if (startsWith1 && !startsWith2) return -1;
            if (!startsWith1 && startsWith2) return 1;

            return title1.compareTo(title2);
        });
        List<Book> booksWithDescriptions = resultBooks.stream()
                .filter(b -> !"No description available".equals(b.getDescription()))
                .toList();

        if (!booksWithDescriptions.isEmpty()) {
            return booksWithDescriptions.stream()
                    .limit(10)
                    .collect(Collectors.toList());
        }

        return resultBooks.stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    private int calculateBookQualityScore(Book book) {
        int score = 0;

        if (!"No description available".equals(book.getDescription())) {
            score += 10;
            score += Math.min(5, book.getDescription().length() / 100);
        }

        if (book.getGenre() != null && book.getGenre().length > 0) {
            score += 5 * book.getGenre().length;
        }

        if (book.getCoverURL() != null && !"/images/template.avif".equals(book.getCoverURL())) {
            score += 3;
        }
        if (book.getPublicationYear() != null && book.getPublicationYear() > 0) {
            score += 2;
        }

        return score;
    }

    /**
     * Helper method to extract work key from a book key
     */
    private String extractWorkKeyFromBook(String bookKey) {
        String bookUrl = "https://openlibrary.org" + bookKey + ".json";
        ResponseEntity<Map<String, Object>> bookResponse = restTemplate.exchange(
                bookUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> bookDetails = bookResponse.getBody();
        if (bookDetails != null && bookDetails.containsKey("works")) {
            List<?> works = (List<?>) bookDetails.get("works");
            if (!works.isEmpty() && works.getFirst() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> firstWork = (Map<String, Object>) works.getFirst();
                if (firstWork.containsKey("key")) {
                    return (String) firstWork.get("key");
                }
            }
        }
        return null;
    }

    /**
     * Helper method to set genres from subject nodes
     */
    private void setGenresFromSubjects(JsonNode subjectNodes, Book book) {
        List<OpenLibraryApi.OpenLibrarySubjectDTO> subjects = new ArrayList<>();

        for (JsonNode subjectNode : subjectNodes) {
            OpenLibraryApi.OpenLibrarySubjectDTO subject = new OpenLibraryApi.OpenLibrarySubjectDTO();
            subject.setName(subjectNode.asText());
            subjects.add(subject);
        }

        book.setGenre(bookMapper.formatSubjects(subjects));
    }

    /**
     * Helper method to enhance book with work details
     */
    private void enhanceBookWithWorkDetails(Book book, String workKey) {
        try {
            Map<String, Object> workDetails = fetchWorkDetails(workKey);
            if (workDetails != null) {

                // Description
                if (workDetails.containsKey("description")) {
                    Object descObj = workDetails.get("description");
                    String description = getDescription(descObj);
                    if (!description.isEmpty()) {
                        book.setDescription(description);
                    }
                }

                // Subjects/genres if not already set
                if (workDetails.containsKey("subjects") &&
                        (book.getGenre() == null || book.getGenre().length == 0)) {
                    List<?> subjects = (List<?>) workDetails.get("subjects");
                    List<String> genres = getStrings(subjects);
                    if (!genres.isEmpty()) {
                        book.setGenre(genres.toArray(new String[0]));
                    }
                }

                // Store in cache
                if (!workDetailsCache.containsKey(workKey)) {
                    workDetailsCache.put(workKey, workDetails);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching work details: " + e.getMessage());
        }
    }

    /**
     * Compares two book objects and determines which one has better quality data
     */
    private boolean isBookBetterQuality(Book newBook, Book existingBook) {
        int currentScore = 0;
        int newScore = 0;

        if (!"No description available".equals(existingBook.getDescription())) currentScore += 5;
        if (!"No description available".equals(newBook.getDescription())) newScore += 5;

        if (existingBook.getGenre() != null && existingBook.getGenre().length > 0) currentScore += 3;
        if (newBook.getGenre() != null && newBook.getGenre().length > 0) newScore += 3;

        if (existingBook.getPublicationYear() != null && existingBook.getPublicationYear() > 0) currentScore += 2;
        if (newBook.getPublicationYear() != null && newBook.getPublicationYear() > 0) newScore += 2;

        if (!"/images/template.avif".equals(existingBook.getCoverURL())) currentScore += 1;
        if (!"/images/template.avif".equals(newBook.getCoverURL())) newScore += 1;

        return newScore > currentScore;
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
                    new ParameterizedTypeReference<>() {
                    }
            );

            OpenLibraryApi bookData = Objects.requireNonNull(response.getBody()).get("ISBN:" + isbn);
            if (bookData == null) {
                return null;
            }
            Book book = bookMapper.convertToBook(bookData, isbn);
            book.setIsbn(isbn);

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
                        new ParameterizedTypeReference<>() {
                        }
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
                                        new ParameterizedTypeReference<>() {
                                        }
                                );
                                Map<String, Object> workDetails = workDetailsResponse.getBody();
                                if (workDetails != null) {
                                    workDetailsCache.put("/works/" + workId, workDetails);

                                    if (workDetails.containsKey("description")) {
                                        Object desObj = workDetails.get("description");
                                        String description = getDescription(desObj);
                                        book = bookMapper.updateBookWithDescription(book, description);
                                    }

                                    if (workDetails.containsKey("subtitle")) {
                                        Object subObj = workDetails.get("subtitle");
                                        String subtitle = getSubtitle(subObj);
                                        book.setSubtitle(subtitle);
                                    }
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
            book.setIsbn(isbn);

            return sanitizeBookFields(book);
        } catch (Exception e) {
            System.err.println("Error fetching book data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<OpenLibraryTrendingResponse.TrendingBook> fetchTrendingBooks(int limit) {
        String apiUrl = "https://openlibrary.org/trending/weekly.json?limit=" + limit;

        ResponseEntity<OpenLibraryTrendingResponse> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                OpenLibraryTrendingResponse.class
        );

        if (response.getBody() == null || response.getBody().getWorks() == null) {
            return List.of();
        }

        return response.getBody().getWorks();
    }

    public static String getDescription(Object descObj) {
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

    public String fetchDescriptionFromWorkId(String workId) {
        Map<String, Object> workDetails = fetchWorkDetails(workId);
        if (workDetails == null) {
            return null;
        }
        if (workDetails.containsKey("description")) {
            Object desObj = workDetails.get("description");
            return getDescription(desObj);
        }

        return "No description available";
    }

    private String fetchAuthorName(String authorKey) {
        try {
            String authorUrl = "https://openlibrary.org" + authorKey + ".json";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    authorUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            Map<String, Object> authorData = response.getBody();
            if (authorData != null && authorData.containsKey("name")) {
                return authorData.get("name").toString();
            }
        } catch (Exception e) {
            System.err.println("Error fetching author name: " + e.getMessage());
        }
        return null;
    }

    private List<String> getStrings(List<?> subjects) {
        if (subjects == null || subjects.isEmpty()) return List.of("Fiction");

        Set<String> primaryGenres = Set.of(
                "fiction", "fantasy", "science fiction", "mystery", "thriller", "horror",
                "romance", "historical fiction", "young adult", "adventure", "epic fantasy",
                "biography", "non-fiction", "history", "science", "philosophy", "poetry",
                "drama", "comedy", "classic", "crime", "dystopian", "urban fantasy",
                "paranormal", "western", "memoir", "contemporary", "literary fiction",
                "suspense", "gothic", "cyberpunk", "steampunk", "mythology", "fairy tale",
                "folklore", "supernatural", "magical realism", "satire", "war", "espionage",
                "action", "short stories", "graphic novel", "coming of age", "historical",
                "political", "psychological", "alternate history", "post-apocalyptic"
        );

        Set<String> genreModifiers = Set.of(
                "epic", "dark", "high", "low", "historical", "urban", "contemporary",
                "literary", "classic", "modern", "gothic", "hard", "soft", "military",
                "paranormal", "erotic", "psychological", "philosophical", "political"
        );

        Set<String> exclusionTerms = Set.of(
                "character", "battle", "seasons", "kings and rulers", "winter", "invierno",
                "imaginary wars", "imaginary places", "award winner", "bestseller", "new york times",
                "courts and courtiers", "civil war", "good and evil", "bien y mal"
        );

        List<String> potentialGenres = new ArrayList<>();
        for (Object subject : subjects) {
            if (subject instanceof String) {
                String genreStr = ((String) subject).toLowerCase().trim();

                boolean shouldSkip = exclusionTerms.stream().anyMatch(genreStr::contains);
                if (shouldSkip) continue;

                if (primaryGenres.contains(genreStr)) {
                    potentialGenres.add(genreStr);
                }
                else {
                    for (String modifier : genreModifiers) {
                        if (genreStr.contains(modifier)) {
                            potentialGenres.add(genreStr);
                            break;
                        }
                    }
                }
            }
        }
        if (!potentialGenres.isEmpty()) {
            return potentialGenres.stream()
                    .distinct()
                    .limit(5)
                    .map(this::capitalizeFirstLetter)
                    .collect(Collectors.toList());
        }
        return List.of("Fiction");
    }

    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
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

    private List<Book> processSearchRequest(String searchUrl) {
        Map<String, Object> result = performSearchRequest(searchUrl);
        if (result == null) return new ArrayList<>();

        // Use a cached thread pool for parallel processing
        ExecutorService executor = Executors.newFixedThreadPool(5);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.valueToTree(result);

        if (jsonNode == null || !jsonNode.has("docs")) return new ArrayList<>();
        JsonNode docs = jsonNode.get("docs");

        // Process only the top 20 results
        int resultLimit = Math.min(20, docs.size());
        List<Future<Book>> futures = new ArrayList<>();

        for (int i = 0; i < resultLimit; i++) {
            JsonNode doc = docs.get(i);
            futures.add(executor.submit(() -> processBookDoc(doc, jsonNode)));
        }

        // Collect and filter results
        List<Book> books = futures.stream()
                .map(future -> {
                    try {
                        return future.get(3, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(this::sanitizeBookFields)
                .collect(Collectors.toList());

        executor.shutdown();
        return books;
    }

    private Book processBookDoc(JsonNode doc, JsonNode root) {
        Book book = new Book();

        book.setTitle(doc.has("title") ? doc.get("title").asText() : "Unknown Title");
        book.setSubtitle("");
        book.setDescription("No description available");

        String author = extractAuthorFromDoc(doc);
        book.setAuthor(author);


        if (doc.has("cover_i")) {
            book.setCoverURL("https://covers.openlibrary.org/b/id/" + doc.get("cover_i").asText() + "-M.jpg");
        } else {
            book.setCoverURL("/images/template.avif");
        }

        if (doc.has("first_publish_year")) {
            book.setPublicationYear(doc.get("first_publish_year").asInt());
        }

        if (doc.has("subject") && doc.get("subject").isArray()) {
            setGenresFromSubjects(doc.get("subject"), book);
        }

        String workKey = null;
        if (doc.has("key") && doc.get("key").asText().startsWith("/works/")) {
            workKey = doc.get("key").asText();
        } else if (doc.has("works") && doc.get("works").isArray() && !doc.get("works").isEmpty()) {
            JsonNode firstWork = doc.get("works").get(0);
            if (firstWork.has("key")) {
                workKey = firstWork.get("key").asText();
            }
        }

        if (workKey != null) {
            try {
                if (!workKey.startsWith("/works/")) {
                    workKey = "/works/" + workKey.replace("/", "");
                }

                Map<String, Object> workDetails = workDetailsCache.get(workKey);
                if (workDetails == null) {
                    workDetails = fetchWorkDetails(workKey);
                }

                if (workDetails != null) {
                    if (workDetails.containsKey("description")) {
                        Object descObj = workDetails.get("description");
                        String description = getDescription(descObj);
                        if (description != null && !description.isEmpty()) {
                            book.setDescription(description);
                        }
                    }

                    if (workDetails.containsKey("subjects") &&
                            (book.getGenre() == null || book.getGenre().length == 0)) {
                        List<?> subjects = (List<?>) workDetails.get("subjects");
                        List<String> genres = getStrings(subjects);
                        if (!genres.isEmpty()) {
                            book.setGenre(genres.toArray(new String[0]));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error enhancing book details for work " + workKey + ": " + e.getMessage());
            }
        }

        String isbn;
        if (doc.has("isbn") && doc.get("isbn").isArray() && !doc.get("isbn").isEmpty()) {
            isbn = normalizeIsbn(doc.get("isbn").get(0).asText());
        } else {
            isbn = workKey != null ? fetchIsbnFromWork(workKey) : "0000000000000";
        }
        book.setIsbn(isbn);

        // Set API ID
        book.setApiId(generateApiIdFromKey(workKey != null ? workKey :
                (doc.has("key") ? doc.get("key").asText() : "unknown")));
        book.setLastSync(LocalDateTime.now());

        return book;
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
     *
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

    private String extractAuthorFromDoc(JsonNode doc) {
        if (doc.has("author_name") && doc.get("author_name").isArray() && !doc.get("author_name").isEmpty()) {
            StringBuilder authorBuilder = new StringBuilder();
            JsonNode authorNodes = doc.get("author_name");
            for (int i = 0; i < authorNodes.size(); i++) {
                if (i > 0) authorBuilder.append(", ");
                authorBuilder.append(authorNodes.get(i).asText());
            }
            return authorBuilder.toString();
        }

        if (doc.has("author_key") && doc.get("author_key").isArray() && !doc.get("author_key").isEmpty()) {
            String authorKey = "/authors/" + doc.get("author_key").get(0).asText();
            String authorName = fetchAuthorName(authorKey);
            if (authorName != null) {
                return authorName;
            }
        }

        return "Unknown Author";
    }

    private void enhanceBookWithBasicDetails(Book book, Map<String, Object> workDetails) {
        if (workDetails.containsKey("description")) {
            Object descObj = workDetails.get("description");
            String description = getDescription(descObj);
            if (!description.isEmpty()) {
                book.setDescription(description);
            }
        }

        if (workDetails.containsKey("subjects") &&
                (book.getGenre() == null || book.getGenre().length == 0)) {
            List<?> subjects = (List<?>) workDetails.get("subjects");
            List<String> genres = getStrings(subjects);
            if (!genres.isEmpty()) {
                book.setGenre(genres.toArray(new String[0]));
            }
        }
    }

    private String extractIsbnFromDoc(JsonNode doc) {
        if (doc.has("isbn") && doc.get("isbn").isArray() && !doc.get("isbn").isEmpty()) {
            return normalizeIsbn(doc.get("isbn").get(0).asText());
        }

        String workKey = extractWorkKeyFromDoc(doc);
        if (workKey != null) {
            if (workDetailsCache.containsKey(workKey)) {
                Map<String, Object> details = workDetailsCache.get(workKey);
                String isbn = extractIsbnFromWorkDetails(details);
                if (isbn != null) return isbn;
            }
        }

        return generateIsbnFromKey(workKey != null ? workKey :
                (doc.has("key") ? doc.get("key").asText() : "unknown"));
    }

    private String extractWorkKeyFromDoc(JsonNode doc) {
        if (doc.has("key") && doc.get("key").asText().startsWith("/works/")) {
            return doc.get("key").asText();
        }
        if (doc.has("works") && doc.get("works").isArray() && !doc.get("works").isEmpty()) {
            JsonNode firstWork = doc.get("works").get(0);
            if (firstWork.has("key")) {
                return firstWork.get("key").asText();
            }
        }
        return null;
    }

}
