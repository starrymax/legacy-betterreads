package com.br.betterreads.service;

import com.br.betterreads.model.OpenLibraryApi;
import com.br.betterreads.util.BookMapper;
import com.br.betterreads.model.Book;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
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
        String apiUrl = String.format(
                "https://openlibrary.org/api/books?bibkeys=ISBN:%s&format=json&jscmd=data",
                isbn
        );

        ResponseEntity<Map<String, OpenLibraryApi>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, OpenLibraryApi>>() {}
        );

        OpenLibraryApi bookDTO = Objects.requireNonNull(response.getBody()).get("ISBN:" + isbn);
        if (bookDTO == null) {
            throw new RuntimeException("Book not found");
        }

        return bookMapper.convertToBook(bookDTO, isbn);
    }
}