package com.br.betterreads.util;

import com.br.betterreads.model.OpenLibraryApi;
import com.br.betterreads.model.Book;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookMapper {

    public Book convertToBook(OpenLibraryApi dto, String isbn) {
        Book book = new Book();
        book.setTittel(dto.getTitle());
        book.setAuthor(formatAuthors(dto.getAuthors()));
        book.setISBN(isbn);
        book.setCoverURL(dto.getCover() != null ? dto.getCover().getMedium() : null);
        book.setDescription(dto.getDescription());
        book.setGenre(formatSubjects(dto.getSubjects()));
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
}
