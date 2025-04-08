package com.br.betterreads.controller;

import com.br.betterreads.model.Book;
import com.br.betterreads.model.Review;
import com.br.betterreads.model.User;
import com.br.betterreads.model.ValidationResult;
import com.br.betterreads.repository.BookRepository;
import com.br.betterreads.service.ReviewService;
import com.br.betterreads.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ReviewController {

    private final UserService userService;
    private final BookRepository bookRepository;
    private final ReviewService reviewService;

    public ReviewController(UserService userService, BookRepository bookRepository, ReviewService reviewService) {
        this.userService = userService;
        this.bookRepository = bookRepository;
        this.reviewService = reviewService;
    }

    @PostMapping("/book/review/create")
    public String review(@ModelAttribute Review review,
                         @RequestParam String isbn,
                         @RequestParam String text,
                         @RequestParam int rating,
                         RedirectAttributes ra,
                         HttpSession session) {

        User user = userService.getLoggedInUser(session);
        List<Book> books = bookRepository.findByIsbn(isbn);
        Book book = books.isEmpty() ? null : books.getFirst();
        ValidationResult result = reviewService.createAndSaveReview(user, book, rating, text);
        if (!result.valid()) {
            ra.addFlashAttribute("error", result.errorMessage());
            return "redirect:/book?isbn=" + isbn;
        }
        return "redirect:/book?isbn=" + isbn;
    }

    @PostMapping("/book/review/delete")
    public String deleteReview(@RequestParam String isbn) {
        return "redirect:/book?isbn=" + isbn;
    }

    @PostMapping("/book/review/update")
    public String updateReview(@RequestParam String isbn,
                               RedirectAttributes ra,
                               HttpSession session) {
        return "redirect:/book?isbn=" + isbn;
    }

}
