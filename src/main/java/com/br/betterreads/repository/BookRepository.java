package com.br.betterreads.repository;

import com.br.betterreads.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    /**
     * @param isbn
     * @return
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * @param title
     * @return
     */
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Book> findByTitle(String title);

    /**
     * @param subtitle
     * @return
     */
    List<Book> findBySubtitle(String subtitle);

    /**
     * @param author
     * @return
     */
    @Query("SELECT b FROM Book b WHERE LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))")
    List<Book> findBookByAuthor(String author);

    @Query("SELECT b FROM Book b WHERE b.lastSync > :timestamp AND b.trending = true")
    List<Book> findByLastSyncAfterAndTrendingTrue(LocalDateTime timestamp);

    @Modifying
    @Query("UPDATE Book b SET b.trending = false")
    void resetTrendingFlags();
}
