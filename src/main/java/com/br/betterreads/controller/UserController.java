package com.br.betterreads.controller;

import com.br.betterreads.model.Book;
import com.br.betterreads.model.User;
import com.br.betterreads.model.ValidationResult;
import com.br.betterreads.service.ApiService;
import com.br.betterreads.service.BookService;
import com.br.betterreads.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class UserController {

    private final UserService userService;
    private final BookService bookService;
    private final ApiService apiService;

    public UserController(UserService userService, BookService bookService, ApiService apiService) {
        this.userService = userService;
        this.bookService = bookService;
        this.apiService = apiService;
    }

    @GetMapping("/signup")
    public String showSignUpForm(Model model) {
        model.addAttribute("user", new User());
        return "Signup";
    }

    @PostMapping("/signup")
    public String register(@Valid @ModelAttribute("user") User user,
                           Model model) {

        ValidationResult result = userService.createAndSaveUser(user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRepeatPassword());

        if (!result.valid()) {
            model.addAttribute("error", result.errorMessage());
            return "Signup";
        }

        return "redirect:login";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("user", new User());
        return "Login";
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("user") User user,
                               BindingResult bindingResult,
                               HttpSession session,
                               Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Invalid input");
            return "Login";
        }

        ValidationResult result = userService.validateUser(user.getEmail(), user.getPassword(), session);

        if (!result.valid()) {
            model.addAttribute("error", result.errorMessage());
            return "Login";
        }

        return "redirect:/";

    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/")
    public String showMainPage(Model model, HttpSession session) {
        User loggedInUser = userService.getLoggedInUser(session);

//        if (loggedInUser == null) {
//            return "redirect:login";
//        }

        if (loggedInUser != null) {
            model.addAttribute("username", loggedInUser.getUsername());
        }

        List<Book> trendingBooks = bookService.displayTrending(12);
        model.addAttribute("books", trendingBooks);
        return "BetterReads";
    }

}
