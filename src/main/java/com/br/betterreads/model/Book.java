package com.br.betterreads.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Book")
public class Book {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Book_id")
    private int book_id;

    @NotNull
    @Column(name = "Api_id", nullable = false)
    private int api_id;

    @NotNull
    @Column(name = "Title", nullable = false)
    private String title;

    @NotNull
    @Column(name = "Author", nullable = false)
    private String author;

    @NotNull(message = "Field is required")
    @Size(min = 13, max = 13, message = "ISBN must be exactly size 13")
    @Column(name = "ISBN", nullable = false, unique = true)
    private String ISBN;

    private String description;

    @Column(name = "genre")
    private String Genre;

    @NotNull
    @Column(name = "Cover_URL")
    private String coverURL;

    @NotNull(message = "Field is required")
    private LocalDateTime lastSync;


    //Constructors
    public Book(){

    }

    public Book(int Bok_id, int Api_id, String title, String author,
                String ISBN, String Cover_URL, LocalDateTime lastSync){
        this.book_id = Bok_id;
        this.api_id = Api_id;
        this.title = title;
        this.author = author;
        this.ISBN = ISBN;
        this.coverURL = Cover_URL;
        this.lastSync = lastSync;
    }

    //Getters and setters
    public int getBook_id(){
        return book_id;
    }

    public void setBook_id(int Bok_id){
        this.book_id = Bok_id;
    }
    
    public int getApi_id(){
        return api_id;
    }

    public void setApi_id(int Api_id){
        this.api_id = Api_id;
    }

    public String getTittel(){
        return title;
    }

    public void setTittel(String Tittel){
        this.title = Tittel;
    }

    public String getAuthor(){
        return author;
    }

    public void setAuthor(String Forfatter){
        this.author = Forfatter;
    }

    public String getISBN(){
        return ISBN;
    }

    public void setISBN(String ISBN){
        this.ISBN = ISBN;
    }

    public String getCoverURL(){
        return coverURL;
    }

    public void setCoverURL(String Cover_URL){
        this.coverURL = Cover_URL;
    }

    public LocalDateTime getSist_Synced(){
        return lastSync;
    }

    public void setSist_Synced(LocalDateTime Sist_synced){
        this.lastSync = Sist_synced;
    }

    public void setLastSync(LocalDateTime now) {
        this.lastSync = now;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public void setGenre (String Genre){
        this.Genre = Genre;
    }

    public String getGenre(){
        return Genre;
    }
}