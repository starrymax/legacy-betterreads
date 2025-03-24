package com.br.betterreads.controller;

import com.br.betterreads.model.Book;
import com.br.betterreads.repository.BookRepository;
import com.br.betterreads.service.ApiService;
import com.br.betterreads.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class BookController {

    private BookRepository bookRepo;
    private final BookService bookService;
    private ApiService apiService;


    public BookController(BookService bookService, ApiService apiService, BookRepository bookRepo) {
        this.bookService = bookService;
        this.apiService = apiService;
        this.bookRepo = bookRepo;
    }

    @GetMapping("/search")
    public String searchBook(
            @RequestParam String query,
            @RequestParam(defaultValue = "isbn") String searchType, Model model) {
                if (searchType.equals("isbn")) {
                    Book book = bookService.searchBookByIsbn(query);
                    if (book != null) {
                        model.addAttribute("book", book);
                        return "Bok";
                    }
                } else {
                    List<Book> books = bookService.searchBooks(query, searchType);
                    if (!books.isEmpty()) {
                        if (books.size() == 1) {
                            model.addAttribute("book", books.getFirst());
                            return "Bok";
                        }
                        model.addAttribute("books", books);
                        model.addAttribute("query", query);
                        return "searchResults";
                    }
                }
                model.addAttribute("error", "No books found for: " + query);
                return "searchError";
    }

    @GetMapping("/book")
    public String viewBook(@RequestParam String isbn, Model model) {
        Book book = bookService.searchBookByIsbn(isbn);
        if (book != null) {
            model.addAttribute("book", book);
            return "Bok";
        } else {
            model.addAttribute("error", "Book not found");
            return "searchError";
        }
    }
}
