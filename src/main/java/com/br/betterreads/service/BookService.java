package com.br.betterreads.service;

import com.br.betterreads.model.Book;
import com.br.betterreads.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepo;
    private final ApiService apiService;

    public BookService(BookRepository bookRepo, ApiService apiService) {
        this.bookRepo = bookRepo;
        this.apiService = apiService;
    }

    public Book searchBookByIsbn(String isbn) {
        Optional<Book> bookOpt = bookRepo.findByIsbn(isbn);
        if (bookOpt.isPresent()) {
            return bookOpt.get();
        } else {
            Book apiBook = apiService.fetchBookFromApi(isbn);
            if (apiBook != null) {
                bookRepo.save(apiBook);
                return apiBook;
            }
            return null;
        }
    }

    public List<Book> searchBooksByTitle(String title) {
        List<Book> books = bookRepo.findByTitle(title);
        if (books.isEmpty()) {
            List<Book> apiBooks = apiService.searchBookByTitle(title);
            if (!apiBooks.isEmpty()) {
                bookRepo.saveAll(apiBooks);
                return apiBooks;
            }
        }
        return books;
    }

    public List<Book> searchBooksByAuthor(String author) {
        List<Book> books = bookRepo.findBookByAuthor(author);
        if (books.isEmpty()) {
            List<Book> apiBooks = apiService.searchBookByAuthor(author);
            if (!apiBooks.isEmpty()) {
                bookRepo.saveAll(apiBooks);
                return apiBooks;
            }
        }
        return books;
    }

    public List<Book> searchBooks(String query, String searchType) {
        return switch (searchType) {
            case "title" -> searchBooksByTitle(query);
            case "author" -> searchBooksByAuthor(query);
            case "isbn" -> {
                Book book = searchBookByIsbn(query);
                yield book != null ? List.of(book) : List.of();
            }
            default -> List.of();
        };
    }
}
