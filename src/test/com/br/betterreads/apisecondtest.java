package com.br.betterreads;

import com.br.betterreads.model.Book;
import com.br.betterreads.service.ApiService;
import com.br.betterreads.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class apisecondtest {

    @Autowired
    private ApiService apiService;

    @Autowired
    private BookService bookService;

    @Test
    void testSearchBookByTitleWithDescription() {
        List<Book> books = apiService.searchBookByTitle("the eye of the world");


        assertFalse(books.isEmpty());


        Book book = books.getFirst();
        System.out.println("Book Title: " + book.getTitle());
        System.out.println("Author: " + book.getAuthor());
        System.out.println("ISBN: " + book.getIsbn());
        System.out.println("Description: " + book.getDescription());
        System.out.println("Genre: " + Arrays.toString(book.getGenre()));


        assertNotNull(book.getDescription());
        assertNotEquals("No description available", book.getDescription());


        assertNotNull(book.getGenre());
        assertTrue(book.getGenre().length > 0);
    }

    @Test
    void testBookDescription() {
        Book book = bookService.searchBookByIsbn("9780812550306"); // The Eye of the World

        System.out.println("Description from API: " + book.getDescription());
        System.out.println("Description length: " +
                (book.getDescription() != null ? book.getDescription().length() : 0));

        assertNotNull(book.getDescription());
        assertNotEquals("No description available", book.getDescription());
    }
}
