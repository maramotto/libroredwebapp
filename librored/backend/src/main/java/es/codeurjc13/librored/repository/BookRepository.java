package es.codeurjc13.librored.repository;

import es.codeurjc13.librored.model.Book;
import es.codeurjc13.librored.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b.genre, COUNT(b) FROM Book b GROUP BY b.genre")
    List<Object[]> countBooksByGenre();

    @Query("SELECT b FROM Book b WHERE b.owner.id = :ownerId AND " + "b.id NOT IN (SELECT l.book.id FROM Loan l WHERE l.status = es.codeurjc13.librored.model.Loan.Status.Active)")
    List<Book> findAvailableBooksByOwnerId(@Param("ownerId") Long ownerId);

    // EntityGraph: ensures that when fetching books, Hibernate also retrieves associated loans. LazyInitializationException.
    @EntityGraph(attributePaths = {"loans"})
    List<Book> findAll();

    // EntityGraph: ensures that when fetching books, Hibernate also retrieves associated loans. LazyInitializationException.
    @EntityGraph(attributePaths = {"loans"})
    List<Book> findByOwner(User owner);

    // This query selects books from genres the user has borrowed but excludes books they have already borrowed.
    // - Count the number of times a user has borrowed books from each genre.
    // - Recommend books based on the most borrowed genre.
    // - Exclude books the user already owns.
    @Query("SELECT b FROM Book b WHERE b.genre IN " +
            "(SELECT lb.genre FROM Loan l JOIN l.book lb WHERE l.borrower.id = :userId " +
            "GROUP BY lb.genre ORDER BY COUNT(lb.id) DESC) " +
            "AND b.id NOT IN (SELECT lb.id FROM Loan l JOIN l.book lb WHERE l.borrower.id = :userId) " +
            "AND b.owner.id <> :userId")
    List<Book> findRecommendedBooks(@Param("userId") Long userId);


}