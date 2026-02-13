package es.codeurjc13.librored.controller;

import es.codeurjc13.librored.dto.LoanDTO;
import es.codeurjc13.librored.model.User;
import es.codeurjc13.librored.service.LoanService;
import es.codeurjc13.librored.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Tag(name = "Loans", description = "Loan management API")
public class LoanRestController {

    private final UserService userService;
    private final LoanService loanService;

    public LoanRestController(UserService userService, LoanService loanService) {
        this.userService = userService;
        this.loanService = loanService;
    }

    // ==================== EXISTING WEB APP ENDPOINT (/api/loans) ====================
    
    @GetMapping("/api/loans/valid-borrowers")
    public ResponseEntity<List<User>> getValidBorrowers(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User lender = userService.getUserByEmail(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

        List<User> borrowers = userService.getValidBorrowers(lender);

        return ResponseEntity.ok(borrowers);
    }

    // ====================  REST API ENDPOINTS (/api/v1/loans) ====================

    @Operation(summary = "Get all loans", description = "Retrieve a paginated list of all loans")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loans retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/v1/loans")
    public ResponseEntity<Map<String, Object>> getAllLoans(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = loanService.getAllLoansDTOPaginated(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get loan by ID", description = "Retrieve a specific loan by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loan found"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @GetMapping("/api/v1/loans/{id}")
    public ResponseEntity<LoanDTO> getLoanById(
            @Parameter(description = "Loan ID") @PathVariable Long id) {
        Optional<LoanDTO> loan = loanService.getLoanByIdDTO(id);
        return loan.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new loan", description = "Create a new loan entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Loan created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid loan data or business rule violation"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/api/v1/loans")
    public ResponseEntity<LoanDTO> createLoan(
            @Parameter(description = "Loan data") @Valid @RequestBody LoanDTO loanDTO) {
        try {
            LoanDTO createdLoan = loanService.createLoanDTO(loanDTO);
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(createdLoan.id())
                    .toUri();
            return ResponseEntity.created(location).body(createdLoan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update loan", description = "Update an existing loan")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loan updated successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found"),
            @ApiResponse(responseCode = "400", description = "Invalid loan data or business rule violation")
    })
    @PutMapping("/api/v1/loans/{id}")
    public ResponseEntity<LoanDTO> updateLoan(
            @Parameter(description = "Loan ID") @PathVariable Long id,
            @Parameter(description = "Updated loan data") @Valid @RequestBody LoanDTO loanDTO) {
        try {
            Optional<LoanDTO> updatedLoan = loanService.updateLoanDTO(id, loanDTO);
            return updatedLoan.map(ResponseEntity::ok)
                              .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Delete loan", description = "Delete a loan by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Loan deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Loan not found")
    })
    @DeleteMapping("/api/v1/loans/{id}")
    public ResponseEntity<Void> deleteLoan(
            @Parameter(description = "Loan ID") @PathVariable Long id) {
        boolean deleted = loanService.deleteLoanDTO(id);
        return deleted ? ResponseEntity.noContent().build() 
                       : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get loans by lender", description = "Retrieve loans where the user is the lender")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loans retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Lender not found")
    })
    @GetMapping("/api/v1/loans/lender/{lenderId}")
    public ResponseEntity<List<LoanDTO>> getLoansByLender(
            @Parameter(description = "Lender ID") @PathVariable Long lenderId) {
        List<LoanDTO> loans = loanService.getLoansByLenderIdDTO(lenderId);
        return ResponseEntity.ok(loans);
    }

    @Operation(summary = "Get loans by borrower", description = "Retrieve loans where the user is the borrower")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loans retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Borrower not found")
    })
    @GetMapping("/api/v1/loans/borrower/{borrowerId}")
    public ResponseEntity<List<LoanDTO>> getLoansByBorrower(
            @Parameter(description = "Borrower ID") @PathVariable Long borrowerId) {
        List<LoanDTO> loans = loanService.getLoansByBorrowerIdDTO(borrowerId);
        return ResponseEntity.ok(loans);
    }

    @Operation(summary = "Get loans by book", description = "Retrieve all loans for a specific book")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Loans retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    @GetMapping("/api/v1/loans/book/{bookId}")
    public ResponseEntity<List<LoanDTO>> getLoansByBook(
            @Parameter(description = "Book ID") @PathVariable Long bookId) {
        List<LoanDTO> loans = loanService.getLoansByBookIdDTO(bookId);
        return ResponseEntity.ok(loans);
    }

}
