package com.br.betterreads.controller;

import com.br.betterreads.model.Book;
import com.br.betterreads.model.Review;
import com.br.betterreads.model.User;
import com.br.betterreads.repository.BookRepository;
import com.br.betterreads.repository.CollectionRepository;
import com.br.betterreads.repository.ReviewRepository;
import com.br.betterreads.service.ApiService;
import com.br.betterreads.service.BookService;
import com.br.betterreads.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class BookController {

    private final BookRepository bookRepo;
    private final BookService bookService;
    private final ApiService apiService;
    private final UserService userService;
    private final ReviewRepository reviewRepository;
    private final CollectionRepository collectionRepository;


    public BookController(BookService bookService, ApiService apiService, BookRepository bookRepo, UserService userService, ReviewRepository reviewRepository, CollectionRepository collectionRepository) {
        this.bookService = bookService;
        this.apiService = apiService;
        this.bookRepo = bookRepo;
        this.userService = userService;
        this.reviewRepository = reviewRepository;
        this.collectionRepository = collectionRepository;
    }

    @GetMapping("/search")
    public String searchBook(
            @RequestParam String query,
            @RequestParam(defaultValue = "isbn") String searchType, Model model,
            HttpSession session) {

        User loggedInUser = userService.getLoggedInUser(session);

        if (loggedInUser != null) {
            model.addAttribute("user", loggedInUser);
        }

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
    public String viewBook(@RequestParam String isbn, Model model, HttpSession session) {
        Book book = bookService.searchBookByIsbn(isbn);
        User loggedInUser = userService.getLoggedInUser(session);

        if (book == null) {
            model.addAttribute("error", "No book found for: " + isbn);
            return "searchError";
        }

        List<Review> reviews = reviewRepository.findByBook(book);
        model.addAttribute("reviews", reviews);

        if (loggedInUser != null) {
            model.addAttribute("user", loggedInUser);

            boolean inCollection = collectionRepository.findCollectionByUserAndBook(loggedInUser, book).isPresent();
            model.addAttribute("inCollection", inCollection);

            Optional<Review> existingReviewOpt = Optional.ofNullable((Review) reviewRepository.getReviewByUserAndBook(loggedInUser, book));
            model.addAttribute("review", existingReviewOpt.orElse(new Review()));
        } else {
            model.addAttribute("review", new Review());
        }

        String cUrl = book.getCoverURL();
        book.setCoverURL(cUrl.replace("-M", "-L"));
        model.addAttribute("book", book);
        return "Bok";
    }
}
