package com.br.betterreads.controller;

import com.br.betterreads.model.User;
import com.br.betterreads.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/signup")
    public String showSignUpForm(Model model){
        model.addAttribute("user", new User());
        return "signup";


    }




}
