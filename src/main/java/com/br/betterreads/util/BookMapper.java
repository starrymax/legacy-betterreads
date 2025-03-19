package com.br.betterreads.util;

import com.br.betterreads.model.OpenLibraryApi;
import com.br.betterreads.model.Book;
import com.br.betterreads.model.OpenLibraryTrendingResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookMapper {

    public Book convertToBook(OpenLibraryApi dto, String isbn) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        book.setAuthor(formatAuthors(dto.getAuthors()));
        book.setIsbn(isbn);
        book.setCoverURL(dto.getCover() != null ? dto.getCover().getMedium() : null);
        book.setDescription(dto.getDescription());
        book.setGenre(formatSubjects(dto.getGenre()));
        book.setLastSync(java.time.LocalDateTime.now());
        return book;
    }

    private String formatAuthors(List<OpenLibraryApi.OpenLibraryAuthorDTO> authors) {
        if (authors == null) return "Unknown";
        return authors.stream()
                .map(OpenLibraryApi.OpenLibraryAuthorDTO::getName)
                .collect(Collectors.joining(", "));
    }

    private String formatSubjects(List<OpenLibraryApi.OpenLibrarySubjectDTO> subjects) {
        if (subjects == null) return null;
        return subjects.stream()
                .map(OpenLibraryApi.OpenLibrarySubjectDTO::getName)
                .collect(Collectors.joining(", "));
    }

    public Book convertTrendingBook(OpenLibraryTrendingResponse.TrendingBook trendingBook) {
        Book book = new Book();
        book.setTitle(trendingBook.getTitle());

        List<String> authorNames = trendingBook.getAuthor_name();
        if(authorNames != null && !authorNames.isEmpty()) {
            book.setAuthor(String.join(", ", authorNames));
        } else {
            book.setAuthor("Unknown");
        }

        book.setPublicationYear(trendingBook.getFirst_published_year());
        book.setCoverURL(trendingBook.getCoverUrl());
        book.setLastSync(LocalDateTime.now());

        return book;
    }
}
