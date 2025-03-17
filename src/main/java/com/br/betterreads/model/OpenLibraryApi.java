package com.br.betterreads.model;

import java.util.List;

public class OpenLibraryApi {
    private String title;
    private List<OpenLibraryAuthorDTO> authors;
    private OpenLibraryCoverDTO cover;
    private List<OpenLibrarySubjectDTO> subjects;
    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<OpenLibraryAuthorDTO> getAuthors() { return authors; }
    public void setAuthors(List<OpenLibraryAuthorDTO> authors) { this.authors = authors; }

    public OpenLibraryCoverDTO getCover() { return cover; }
    public void setCover(OpenLibraryCoverDTO cover) { this.cover = cover; }

    public List<OpenLibrarySubjectDTO> getSubjects() { return subjects; }
    public void setSubjects(List<OpenLibrarySubjectDTO> subjects) { this.subjects = subjects; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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
}
