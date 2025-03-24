package com.br.betterreads.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

/**
 * Represents a book in the BetterReads application.
 *
 * This entity stores book details such as title, author, ISBN, and genre.
 * It also tracks the book's synchronization status with external systems.
 */
@Entity
@Table(name = "book", schema = "betterreads")
public class Book {

    /**
     * Unique identifier for the book (Auto-generated).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long bookId;

    /**
     * External API identifier for the book.
     */
    @NotNull
    @Column(name = "api_id", nullable = false)
    private int apiId;

    /**
     * Title of the book.
     */
    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * Title of the book.
     */
    @Column(name = "subtitle")
    private String subtitle;


    /**
     * Author of the book.
     */
    @NotNull
    @Column(name = "author", nullable = false, columnDefinition = "varchar(255) default 'Unknown'")
    private String author;

    /**
     * Description of the book's content.
     */
    @Column(name = "description")
    private String description;

    /**
     * Genre of the book (e.g., Fiction, Mystery, etc.).
     */
    @Column(name = "genre", columnDefinition = "text[]")
    @Type(value = com.vladmihalcea.hibernate.type.array.StringArrayType.class)
    private String[] genre;

    @Column(name = "publication_year")
    private Integer publicationYear;

    /**
     * URL to the book's cover image.
     */
    @NotNull
    @Column(name = "cover_url", nullable = false)
    private String coverURL;

    /**
     * ISBN (International Standard Book Number) for the book.
     *
     * Must be exactly 13 characters long to ensure valid ISBN format.
     */
    @NotNull(message = "Field is required")
    @Size(min = 13, max = 13, message = "ISBN must be exactly size 13")
    @Column(name = "ISBN", nullable = false, unique = true)
    private String isbn;

    /**
     * The timestamp of the last successful synchronization with external systems.
     */
    @NotNull(message = "Field is required")
    @Column(name = "last_sync", nullable = false)
    private LocalDateTime lastSync;


    @NotNull
    private boolean trending;


    // Constructors


    /**
     * Default constructor required by JPA.
     */
    public Book() {}

    /**
     * Constructor to initialize a new Book object.
     *
     * @param apiId External API identifier for the book.
     * @param title Title of the book.
     * @param author Author of the book.
     * @param isbn ISBN for the book (13 characters).
     * @param coverURL URL of the book's cover.
     * @param lastSync Timestamp of the last synchronization.
     */
    public Book(int apiId, String title, String author,
                String isbn, String coverURL, LocalDateTime lastSync) {
        this.apiId = apiId;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.coverURL = coverURL;
        this.lastSync = lastSync;
    }

    @PrePersist
    protected void onCreate(){
        this.lastSync = LocalDateTime.now();
    }


    // Getters and Setters


    /**
     * Retrieves the unique identifier for the book.
     *
     * @return The book's ID.
     */
    public Long getBookId() {
        return bookId;
    }

    /**
     * Retrieves the external API ID for the book.
     *
     * @return The book's API ID.
     */
    public int getApiId() {
        return apiId;
    }

    /**
     * Sets the external API ID for the book.
     *
     * @param apiId The new API ID to assign.
     */
    public void setApiId(int apiId) {
        this.apiId = apiId;
    }

    /**
     * Retrieves the book's title.
     *
     * @return The book's title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the book's title.
     *
     * @param title The new title to assign.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Retrieves the author of the book.
     *
     * @return The book's author.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the author of the book.
     *
     * @param author The new author to assign.
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Retrieves the ISBN of the book.
     *
     * @return The ISBN (13 characters).
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Sets the ISBN for the book.
     *
     * @param isbn The new ISBN to assign (must be exactly 13 characters).
     */
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    /**
     * Retrieves the book's description.
     *
     * @return The description of the book.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the book.
     *
     * @param description The new description to assign.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Retrieves the book's genre.
     *
     * @return The book's genre.
     */
    public String[] getGenre() {
        return genre;
    }

    /**
     * Sets the genre of the book.
     *
     * @param genre The new genre to assign.
     */
    public void setGenre(String[] genre) {
        this.genre = genre;
    }

    public @NotNull String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(Integer publicationYear) {
        this.publicationYear = publicationYear;
    }

    /**
     * Retrieves the URL of the book's cover image.
     *
     * @return The URL of the book's cover.
     */
    public String getCoverURL() {
        return coverURL;
    }

    /**
     * Sets the URL of the book's cover image.
     *
     * @param coverURL The new URL to assign.
     */
    public void setCoverURL(String coverURL) {
        this.coverURL = coverURL;
    }

    /**
     * Retrieves the timestamp of the book's last synchronization.
     *
     * @return The timestamp of the last sync.
     */
    public LocalDateTime getLastSync() {
        return lastSync;
    }

    /**
     * Sets the timestamp of the book's last synchronization.
     *
     * @param lastSync The new timestamp to assign.
     */
    public void setLastSync(LocalDateTime lastSync) {
        this.lastSync = lastSync;
    }


    public boolean isTrending() {
        return trending;
    }

    public void setTrending(boolean trending) {
        this.trending = trending;
    }

    //toString
    @Override
    public String toString() {
        return "Book{" +
                "bookId=" + bookId +
                ", apiId=" + apiId +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", isbn='" + isbn + '\'' +
                ", description='" + description + '\'' +
                ", genre='" + genre + '\'' +
                ", coverURL='" + coverURL + '\'' +
                ", lastSync=" + lastSync +
                '}';
    }

}
