package com.br.betterreads.service;

import com.br.betterreads.model.Book;
import com.br.betterreads.model.Review;
import com.br.betterreads.model.User;
import com.br.betterreads.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepo;

    public ReviewService(ReviewRepository reviewRepo) {
        this.reviewRepo = reviewRepo;
    }

    public Review createAndSaveReview(User user, Book book, int rating, String text) {
        Review review = new Review(user, book, rating, text);
        return reviewRepo.save(review);
    }

}
