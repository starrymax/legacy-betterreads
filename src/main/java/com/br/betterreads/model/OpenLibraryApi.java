package com.br.betterreads.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenLibraryApi {
    private String title;

    private String subtitle;

    private String url;

    @JsonProperty("authors")
    private List<OpenLibraryAuthorDTO> authors;

    @JsonProperty("cover")
    private OpenLibraryCoverDTO cover;

    @JsonProperty("subjects")
    private List<OpenLibrarySubjectDTO> genre;

    @JsonProperty("publish_date")
    private String publishDate;

    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public List<OpenLibraryAuthorDTO> getAuthors() { return authors; }
    public void setAuthors(List<OpenLibraryAuthorDTO> authors) { this.authors = authors; }

    public OpenLibraryCoverDTO getCover() { return cover; }
    public void setCover(OpenLibraryCoverDTO cover) { this.cover = cover; }

    public List<OpenLibrarySubjectDTO> getGenre() {
        return genre;
    }

    public void setGenre(List<OpenLibrarySubjectDTO> genre) {
        this.genre = genre;
    }

    public static class OpenLibraryAuthorDTO {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class OpenLibraryCoverDTO {
        private String medium;
        public String getMedium() { return medium; }
        public void setMedium(String medium) { this.medium = medium; }
    }

    public static class OpenLibrarySubjectDTO {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
