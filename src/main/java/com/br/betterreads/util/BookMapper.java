package com.br.betterreads.util;

import com.br.betterreads.model.OpenLibraryApi;
import com.br.betterreads.model.Book;
import com.br.betterreads.model.OpenLibraryTrendingResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BookMapper {

    public Book convertToBook(OpenLibraryApi dto, String isbn) {
        Book book = new Book();
        book.setTitle(dto.getTitle());
        if (dto.getSubtitle() != null) {
            book.setSubtitle(dto.getSubtitle());
        } else {
            book.setSubtitle("");
        }
        book.setAuthor(formatAuthors(dto.getAuthors()));
        book.setIsbn(isbn);
        book.setCoverURL(dto.getCover() != null ? dto.getCover().getMedium() : "/images/template.avif");
        book.setGenre(formatSubjects(dto.getGenre()));
        book.setLastSync(LocalDateTime.now());

        if (dto.getDescription() != null) {
            book.setDescription(dto.getDescription());
        } else {
            book.setDescription("No description available");
        }

        if (dto.getPublishDate() != null) {
            try {
                String[] split = dto.getPublishDate().split(" ");
                String year = split[split.length - 1];
                book.setPublicationYear(Integer.parseInt(year));
            } catch (Exception e) {
                book.setPublicationYear(null);
            }
        }
        return book;
    }

    private String formatAuthors(List<OpenLibraryApi.OpenLibraryAuthorDTO> authors) {
        if (authors == null) return "Unknown";
        return authors.stream()
                .map(OpenLibraryApi.OpenLibraryAuthorDTO::getName)
                .collect(Collectors.joining(", "));
    }

    public String[] formatSubjects(List<OpenLibraryApi.OpenLibrarySubjectDTO> subjects) {
        if (subjects == null || subjects.isEmpty()) return new String[0];
        Set<String> unique = subjects.stream()
                .map(OpenLibraryApi.OpenLibrarySubjectDTO::getName)
                .map(String::toLowerCase)
                .filter(genre -> !genre.contains("fictitious character"))
                .filter(genre -> genre.matches("^[a-zA-Z ]+$"))
                .collect(Collectors.toSet());

        return unique.isEmpty() ? new String[0] : unique.toArray(new String[0]);
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

    public Book updateBookWithDescription(Book book, String description) {
        if (description != null && !description.isEmpty()) {
            book.setDescription(description);
        }
        return book;
    }
}
