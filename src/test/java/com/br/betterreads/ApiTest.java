package com.br.betterreads;

import com.br.betterreads.model.Book;
import com.br.betterreads.service.ApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ApiTest {

    @Autowired
    private ApiService apiService;

    @Test
    void testFetchBook() {
        Book book = apiService.fetchBookFromApi("9780812550306");

        System.out.println("Book details:");
        System.out.println("Title: " + book.getTitle());
        System.out.println("Subtitle: " + book.getSubtitle());
        System.out.println("Author: " + book.getAuthor());
        System.out.println("ISBN: " + book.getIsbn());
        System.out.println("Genre: " + Arrays.toString(book.getGenre()));
        System.out.println("covers: " + book.getCoverURL());
        System.out.println("Publication Year: " + book.getPublicationYear());
        System.out.println("Description: " + book.getDescription());

        assertNotNull(book.getTitle());
        assertEquals("Robert Jordan", book.getAuthor());
    }
}