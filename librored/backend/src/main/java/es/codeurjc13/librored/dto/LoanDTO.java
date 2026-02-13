package es.codeurjc13.librored.dto;

import es.codeurjc13.librored.model.Loan;
import java.time.LocalDate;

/**
 * Loan DTO for REST API operations
 * Contains complete loan information with basic references to related entities
 */
public record LoanDTO(
        Long id,
        BookBasicDTO book,
        UserBasicDTO lender,
        UserBasicDTO borrower,
        LocalDate startDate,
        LocalDate endDate,
        Loan.Status status) {
}