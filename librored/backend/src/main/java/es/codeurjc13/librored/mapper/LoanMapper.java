package es.codeurjc13.librored.mapper;

import es.codeurjc13.librored.dto.LoanDTO;
import es.codeurjc13.librored.model.Loan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.List;

/**
 * Mapper for converting between Loan entities and DTOs
 */
@Component
public class LoanMapper {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BookMapper bookMapper;

    /**
     * Convert Loan entity to LoanDTO
     */
    public LoanDTO toDTO(Loan loan) {
        if (loan == null) {
            return null;
        }
        return new LoanDTO(
                loan.getId(),
                bookMapper.toBasicDTO(loan.getBook()),
                userMapper.toBasicDTO(loan.getLender()),
                userMapper.toBasicDTO(loan.getBorrower()),
                loan.getStartDate(),
                loan.getEndDate(),
                loan.getStatus()
        );
    }

    /**
     * Convert LoanDTO to Loan entity
     */
    public Loan toDomain(LoanDTO loanDTO) {
        if (loanDTO == null) {
            return null;
        }
        Loan loan = new Loan();
        loan.setId(loanDTO.id());
        loan.setStartDate(loanDTO.startDate());
        loan.setEndDate(loanDTO.endDate());
        loan.setStatus(loanDTO.status());
        // Note: Book, Lender, and Borrower are typically set separately in services
        return loan;
    }

    /**
     * Convert collection of Loan entities to DTOs
     */
    public List<LoanDTO> toDTOs(Collection<Loan> loans) {
        return loans.stream()
                .map(this::toDTO)
                .toList();
    }
}