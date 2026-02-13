package es.codeurjc13.librored.service;

import es.codeurjc13.librored.dto.LoanDTO;
import es.codeurjc13.librored.mapper.LoanMapper;
import es.codeurjc13.librored.model.Book;
import es.codeurjc13.librored.model.Loan;
import es.codeurjc13.librored.model.User;
import es.codeurjc13.librored.repository.BookRepository;
import es.codeurjc13.librored.repository.LoanRepository;
import es.codeurjc13.librored.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final LoanMapper loanMapper;
    private final PlatformTransactionManager transactionManager;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, 
                      UserRepository userRepository, LoanMapper loanMapper, 
                      PlatformTransactionManager transactionManager) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.loanMapper = loanMapper;
        this.transactionManager = transactionManager;
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public Optional<Loan> getLoanById(Long id) {
        return loanRepository.findById(id);
    }


    public void createLoan(Book book, User lender, User borrower, LocalDate startDate, LocalDate endDate, Loan.Status status) {
        Loan loan = new Loan(book, lender, borrower, startDate, endDate, status);
        loanRepository.save(loan);
    }

    public void deleteLoan(Long id) {
        // Use manual transaction management for reliable deletion
        TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
        
        try {
            Optional<Loan> loan = loanRepository.findById(id);
            if (loan.isPresent()) {
                // Use JPQL for reliable cross-database compatibility
                int rowsAffected = entityManager.createQuery("DELETE FROM Loan l WHERE l.id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
                
                entityManager.flush();
                
                if (rowsAffected == 0) {
                    transactionManager.rollback(transaction);
                    throw new RuntimeException("Database constraint preventing deletion");
                }
                
                // Commit the transaction immediately
                transactionManager.commit(transaction);
                
            } else {
                transactionManager.rollback(transaction);
                throw new RuntimeException("Loan not found");
            }
            
        } catch (Exception e) {
            if (!transaction.isCompleted()) {
                transactionManager.rollback(transaction);
            }
            throw new RuntimeException("Failed to delete loan: " + e.getMessage());
        }
    }

    public void saveLoan(Loan loan) {
        loanRepository.save(loan);
    }

    public List<Loan> getLoansByLender(User lender) {
        return loanRepository.findByLender(lender);
    }

    /**
     * Check if a book is available during a specific date range (excluding current loan being edited)
     * @param bookId The book to check
     * @param startDate Start date of the period
     * @param endDate End date of the period (can be null for open-ended loans)
     * @param excludeLoanId Loan ID to exclude from the check (when editing existing loan)
     * @return true if book is available, false if there's a conflict
     */
    public boolean isBookAvailableForDateRange(Long bookId, LocalDate startDate, LocalDate endDate, Long excludeLoanId) {
        List<Loan> overlappingLoans = loanRepository.findOverlappingLoans(bookId, startDate, endDate, excludeLoanId);
        return overlappingLoans.isEmpty();
    }

    /**
     * Check if a borrower already has an active loan from the same lender during the date range
     */
    public boolean isBorrowerAvailableForDateRange(Long borrowerId, Long lenderId, LocalDate startDate, LocalDate endDate, Long excludeLoanId) {
        List<Loan> overlappingBorrowerLoans = loanRepository.findOverlappingBorrowerLoans(borrowerId, lenderId, startDate, endDate, excludeLoanId);
        return overlappingBorrowerLoans.isEmpty();
    }

    // ==================== DTO-BASED METHODS FOR REST API ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getAllLoansDTOPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Loan> loanPage = loanRepository.findAllWithDetails(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", loanMapper.toDTOs(loanPage.getContent()));
        response.put("currentPage", loanPage.getNumber());
        response.put("totalPages", loanPage.getTotalPages());
        response.put("totalItems", loanPage.getTotalElements());
        response.put("hasNext", loanPage.hasNext());
        response.put("hasPrevious", loanPage.hasPrevious());
        response.put("isFirst", loanPage.isFirst());
        response.put("isLast", loanPage.isLast());
        
        return response;
    }

    public Optional<LoanDTO> getLoanByIdDTO(Long id) {
        Optional<Loan> loan = loanRepository.findById(id);
        return loan.map(loanMapper::toDTO);
    }

    public LoanDTO createLoanDTO(LoanDTO loanDTO) {
        Loan loan = loanMapper.toDomain(loanDTO);
        
        // Set book, lender, and borrower from DTO references
        if (loanDTO.book() != null) {
            Book book = bookRepository.findById(loanDTO.book().id())
                    .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + loanDTO.book().id()));
            loan.setBook(book);
        }
        
        if (loanDTO.lender() != null) {
            User lender = userRepository.findById(loanDTO.lender().id())
                    .orElseThrow(() -> new IllegalArgumentException("Lender not found with id: " + loanDTO.lender().id()));
            loan.setLender(lender);
        }
        
        if (loanDTO.borrower() != null) {
            User borrower = userRepository.findById(loanDTO.borrower().id())
                    .orElseThrow(() -> new IllegalArgumentException("Borrower not found with id: " + loanDTO.borrower().id()));
            loan.setBorrower(borrower);
        }
        
        // Validate business rules
        validateLoanBusinessRules(loan);
        
        Loan savedLoan = loanRepository.save(loan);
        return loanMapper.toDTO(savedLoan);
    }

    public Optional<LoanDTO> updateLoanDTO(Long id, LoanDTO loanDTO) {
        System.out.println("=== UPDATE LOAN DTO DEBUG ===");
        System.out.println("Updating loan ID: " + id);
        System.out.println("Received LoanDTO: " + loanDTO.toString());

        Optional<Loan> existingLoanOpt = loanRepository.findById(id);
        if (existingLoanOpt.isPresent()) {
            Loan loan = existingLoanOpt.get();
            System.out.println("Existing loan found:");
            System.out.println("  - Existing lender ID: " + loan.getLender().getId());
            System.out.println("  - Existing lender username: " + loan.getLender().getUsername());
            System.out.println("  - DTO lender ID: " + (loanDTO.lender() != null ? loanDTO.lender().id() : "null"));
            System.out.println("  - DTO lender username: " + (loanDTO.lender() != null ? loanDTO.lender().username() : "null"));

            // Prevent lender change (Fixed Lender Rule)
            if (loanDTO.lender() != null && !loan.getLender().getId().equals(loanDTO.lender().id())) {
                String errorMsg = "Lender cannot be changed. The loan must remain under " + loan.getLender().getUsername() +
                    ". Attempted to change from ID " + loan.getLender().getId() + " to ID " + loanDTO.lender().id();
                System.out.println("❌ LENDER CHANGE ERROR: " + errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
            System.out.println("✅ Lender validation passed");

            // Update book if provided
            if (loanDTO.book() != null) {
                System.out.println("📖 Updating book - DTO book ID: " + loanDTO.book().id());
                try {
                    Optional<Book> book = bookRepository.findById(loanDTO.book().id());
                    if (book.isPresent()) {
                        System.out.println("📖 Book found in database: " + book.get().getTitle());

                        // Check if book is owned by the lender
                        if (!book.get().getOwner().equals(loan.getLender())) {
                            String errorMsg = "The selected book is not owned by " + loan.getLender().getUsername();
                            System.out.println("❌ BOOK OWNERSHIP ERROR: " + errorMsg);
                            throw new IllegalArgumentException(errorMsg);
                        }

                        // Check if book is available for the date range (excluding current loan)
                        boolean isAvailable = isBookAvailableForDateRange(book.get().getId(), loan.getStartDate(), loan.getEndDate(), loan.getId());
                        System.out.println("📖 Book availability check (excluding current loan): " + isAvailable);

                        if (!isAvailable) {
                            String errorMsg = "The selected book is currently loaned out during the selected date range. Please choose another book or adjust the dates.";
                            System.out.println("❌ BOOK AVAILABILITY ERROR: " + errorMsg);
                            throw new IllegalArgumentException(errorMsg);
                        }

                        loan.setBook(book.get());
                        System.out.println("✅ Book updated successfully");
                    } else {
                        String errorMsg = "Book not found with id: " + loanDTO.book().id();
                        System.out.println("❌ BOOK NOT FOUND ERROR: " + errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                } catch (Exception e) {
                    System.out.println("❌ EXCEPTION in book update: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    throw e;
                }
            }

            // Update borrower if provided
            if (loanDTO.borrower() != null) {
                System.out.println("👤 Updating borrower - DTO borrower ID: " + loanDTO.borrower().id());
                try {
                    Optional<User> borrower = userRepository.findById(loanDTO.borrower().id());
                    if (borrower.isPresent()) {
                        System.out.println("👤 Borrower found: " + borrower.get().getUsername());
                        loan.setBorrower(borrower.get());
                        System.out.println("✅ Borrower updated successfully");
                    } else {
                        String errorMsg = "Borrower not found with id: " + loanDTO.borrower().id();
                        System.out.println("❌ BORROWER NOT FOUND ERROR: " + errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                } catch (Exception e) {
                    System.out.println("❌ EXCEPTION in borrower update: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    throw e;
                }
            }

            // Update dates
            if (loanDTO.startDate() != null) {
                System.out.println("📅 Updating start date: " + loanDTO.startDate());
                try {
                    loan.setStartDate(loanDTO.startDate());
                    System.out.println("✅ Start date updated successfully");
                } catch (Exception e) {
                    System.out.println("❌ EXCEPTION in start date update: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    throw e;
                }
            }
            if (loanDTO.endDate() != null) {
                System.out.println("📅 Updating end date: " + loanDTO.endDate());
                try {
                    if (loanDTO.endDate().isBefore(loan.getStartDate())) {
                        String errorMsg = "End date cannot be before the start date. Please select a date after " + loan.getStartDate() + ".";
                        System.out.println("❌ END DATE VALIDATION ERROR: " + errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                    loan.setEndDate(loanDTO.endDate());
                    System.out.println("✅ End date updated successfully");
                } catch (Exception e) {
                    System.out.println("❌ EXCEPTION in end date update: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    throw e;
                }
            }

            // Update status with validation
            if (loanDTO.status() != null) {
                System.out.println("📊 Updating status from " + loan.getStatus() + " to " + loanDTO.status());
                try {
                    if (loan.getStatus() == Loan.Status.Completed && loanDTO.status() == Loan.Status.Active) {
                        String errorMsg = "A completed loan cannot be reactivated. Consider creating a new loan instead.";
                        System.out.println("❌ STATUS VALIDATION ERROR: " + errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                    loan.setStatus(loanDTO.status());
                    System.out.println("✅ Status updated successfully");
                } catch (Exception e) {
                    System.out.println("❌ EXCEPTION in status update: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    throw e;
                }
            }

            System.out.println("💾 Attempting to save loan...");
            try {
                Loan savedLoan = loanRepository.save(loan);
                System.out.println("✅ Loan saved successfully with ID: " + savedLoan.getId());
                return Optional.of(loanMapper.toDTO(savedLoan));
            } catch (Exception e) {
                System.out.println("❌ EXCEPTION during loan save: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
        return Optional.empty();
    }

    public boolean deleteLoanDTO(Long id) {
        // Use the same reliable deletion method as the entity version
        try {
            deleteLoan(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<LoanDTO> getLoansByLenderIdDTO(Long lenderId) {
        Optional<User> lender = userRepository.findById(lenderId);
        if (lender.isPresent()) {
            List<Loan> loans = loanRepository.findByLender(lender.get());
            return loanMapper.toDTOs(loans);
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public List<LoanDTO> getLoansByBorrowerIdDTO(Long borrowerId) {
        Optional<User> borrower = userRepository.findById(borrowerId);
        if (borrower.isPresent()) {
            List<Loan> loans = loanRepository.findByBorrower(borrower.get());
            return loanMapper.toDTOs(loans);
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public List<LoanDTO> getLoansByBookIdDTO(Long bookId) {
        Optional<Book> book = bookRepository.findById(bookId);
        if (book.isPresent()) {
            List<Loan> loans = loanRepository.findByBook(book.get());
            return loanMapper.toDTOs(loans);
        }
        return List.of();
    }

    /**
     * Validate business rules for loan creation/update
     */
    private void validateLoanBusinessRules(Loan loan) {
        // Check book availability for the date range
        if (!isBookAvailableForDateRange(loan.getBook().getId(), loan.getStartDate(), loan.getEndDate(), null)) {
            throw new IllegalArgumentException("Book is not available for the selected date range.");
        }

        // Check borrower availability for the date range
        if (!isBorrowerAvailableForDateRange(loan.getBorrower().getId(), loan.getLender().getId(), 
                loan.getStartDate(), loan.getEndDate(), null)) {
            throw new IllegalArgumentException("Borrower already has an active loan from this lender during the selected date range.");
        }

        // Ensure lender owns the book
        if (!loan.getBook().getOwner().equals(loan.getLender())) {
            throw new IllegalArgumentException("Lender must be the owner of the book.");
        }

        // Ensure lender and borrower are different
        if (loan.getLender().equals(loan.getBorrower())) {
            throw new IllegalArgumentException("Lender and borrower cannot be the same person.");
        }
    }

}

