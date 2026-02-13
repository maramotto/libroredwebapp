package es.codeurjc13.librored.repository;

import es.codeurjc13.librored.model.Book;
import es.codeurjc13.librored.model.Loan;
import es.codeurjc13.librored.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByLender(User lender);

    @Query("SELECT l FROM Loan l WHERE l.borrower = :borrower")
    List<Loan> findByBorrower(@Param("borrower") User borrower);

    @Query("SELECT l FROM Loan l WHERE l.book = :book")
    List<Loan> findByBook(@Param("book") Book book);

    /**
     * Find loans that overlap with the given date range for a specific book
     * Two date ranges overlap if: startA <= endB AND endA >= startB
     * For open-ended loans (endDate is null), we consider them as ongoing indefinitely
     */
    @Query("SELECT l FROM Loan l WHERE l.book.id = :bookId AND l.status = 'Active' " +
           "AND (:excludeLoanId IS NULL OR l.id != :excludeLoanId) " +
           "AND (" +
           "  (l.endDate IS NULL AND :startDate >= l.startDate) OR " +  // Open-ended loan conflicts with any future date
           "  (l.endDate IS NOT NULL AND :endDate IS NULL AND :startDate <= l.endDate) OR " +  // New open-ended conflicts with existing loan
           "  (l.endDate IS NOT NULL AND :endDate IS NOT NULL AND :startDate <= l.endDate AND :endDate >= l.startDate)" +  // Both have end dates
           ")")
    List<Loan> findOverlappingLoans(@Param("bookId") Long bookId, 
                                   @Param("startDate") LocalDate startDate, 
                                   @Param("endDate") LocalDate endDate,
                                   @Param("excludeLoanId") Long excludeLoanId);

    /**
     * Find loans where the same borrower has an active loan from the same lender during the date range
     * This prevents a borrower from having multiple concurrent loans from the same lender
     */
    @Query("SELECT l FROM Loan l WHERE l.borrower.id = :borrowerId AND l.lender.id = :lenderId " +
           "AND l.status = 'Active' AND (:excludeLoanId IS NULL OR l.id != :excludeLoanId) " +
           "AND (" +
           "  (l.endDate IS NULL AND :startDate >= l.startDate) OR " +
           "  (l.endDate IS NOT NULL AND :endDate IS NULL AND :startDate <= l.endDate) OR " +
           "  (l.endDate IS NOT NULL AND :endDate IS NOT NULL AND :startDate <= l.endDate AND :endDate >= l.startDate)" +
           ")")
    List<Loan> findOverlappingBorrowerLoans(@Param("borrowerId") Long borrowerId,
                                           @Param("lenderId") Long lenderId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("excludeLoanId") Long excludeLoanId);

    /**
     * Find all loans with eager loading of related entities (book, lender, borrower)
     * This prevents LazyInitializationException when mapping to DTOs
     */
    @Query("SELECT l FROM Loan l " +
           "JOIN FETCH l.book " +
           "JOIN FETCH l.lender " +
           "JOIN FETCH l.borrower")
    Page<Loan> findAllWithDetails(Pageable pageable);

}
