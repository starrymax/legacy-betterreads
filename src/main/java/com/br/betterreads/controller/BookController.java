package com.br.betterreads.controller;

import com.br.betterreads.model.Book;
import com.br.betterreads.repository.BookRepository;
import com.br.betterreads.service.ApiService;
import com.br.betterreads.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class BookController {

    private BookRepository bookRepo;
    private BookService bookService;
    private ApiService apiService;


}
