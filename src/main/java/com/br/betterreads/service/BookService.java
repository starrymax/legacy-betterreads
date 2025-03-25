package com.br.betterreads.service;

import com.br.betterreads.model.Book;
import com.br.betterreads.model.OpenLibraryTrendingResponse;
import com.br.betterreads.repository.BookRepository;
import com.br.betterreads.util.BookMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private static final long CACHE_DURATION_HOURS = 24;
    private final BookRepository bookRepo;
    private final ApiService apiService;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepo, ApiService apiService, BookMapper bookMapper) {
        this.bookRepo = bookRepo;
        this.apiService = apiService;
        this.bookMapper = bookMapper;
    }

    @Transactional
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

    @Transactional
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

    @Transactional
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

    @Transactional
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

    @Transactional
    public List<Book> displayTrending(int limit) {
        List<Book> cachedBooks = bookRepo.findByLastSyncAfterAndTrendingTrue(
                LocalDateTime.now().minusHours(CACHE_DURATION_HOURS)
        );

        if (!cachedBooks.isEmpty()) {
            return cachedBooks.subList(0, Math.min(limit, cachedBooks.size()));
        }

        List<OpenLibraryTrendingResponse.TrendingBook> trendingBooks = apiService.fetchTrendingBooks(limit);
        if (!trendingBooks.isEmpty()) {
            bookRepo.resetTrendingFlags();
            List<Book> books = trendingBooks.stream()
                    .map(trendingBook -> {
                        Book book = bookMapper.convertTrendingBook(trendingBook);
                        String workId = trendingBook.getKey().replace("/works/", "");
                        String desc = apiService.fetchDescriptionFromWorkId(workId);
                        book.setIsbn(workId);
                        book.setDescription(desc);
                        book.setTrending(true);
                        book.setLastSync(LocalDateTime.now());
                        return book;
                    })
                    .toList();
            return bookRepo.saveAll(books);
        }
        return new ArrayList<>();
    }

}
