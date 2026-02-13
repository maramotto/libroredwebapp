package es.codeurjc13.librored.controller;

import es.codeurjc13.librored.dto.BookBasicDTO;
import es.codeurjc13.librored.dto.BookDTO;
import es.codeurjc13.librored.model.Book;
import es.codeurjc13.librored.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import es.codeurjc13.librored.model.User;
import es.codeurjc13.librored.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@Tag(name = "Books", description = "Book management API")
public class BookRestController {

    private final BookService bookService;
    private final UserService userService;

    public BookRestController(BookService bookService, UserService userService) {
        this.bookService = bookService;
        this.userService = userService;
    }

    // ==================== WEB APP ENDPOINTS (/api/books) ====================

    // Paginated books API - CRITICAL for web app functionality
    @GetMapping("/api/books")
    public ResponseEntity<Map<String, Object>> getBooks(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "8") int size) {
        Page<Book> books = bookService.getBooks(page, size);

        // Convert Page<Book> to a stable JSON format with DTOs to avoid lazy loading issues
        Map<String, Object> response = new HashMap<>();
        response.put("content", books.getContent().stream().map(book -> {
            Map<String, Object> bookMap = new HashMap<>();
            bookMap.put("id", book.getId());
            bookMap.put("title", book.getTitle());
            bookMap.put("author", book.getAuthor());
            bookMap.put("genre", book.getGenre());
            bookMap.put("description", book.getDescription());
            bookMap.put("hasCoverImage", book.getCoverPic() != null);
            if (book.getOwner() != null) {
                Map<String, Object> ownerMap = new HashMap<>();
                ownerMap.put("id", book.getOwner().getId());
                ownerMap.put("username", book.getOwner().getUsername());
                bookMap.put("owner", ownerMap);
            } else {
                bookMap.put("owner", null);
            }
            return bookMap;
        }).toList());
        response.put("currentPage", books.getNumber());
        response.put("totalPages", books.getTotalPages());
        response.put("totalItems", books.getTotalElements());
        response.put("last", books.isLast());

        return ResponseEntity.ok(response);
    }


    @GetMapping("/api/books/{id}/cover")
    public ResponseEntity<Resource> getBookCover(@PathVariable Long id) {
        try {
            Book book = bookService.findBookById(id);

            if (book.getCoverPic() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource file = new InputStreamResource(book.getCoverPic().getBinaryStream());
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "image/jpeg").body(file);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // Books per genre (for graphs or analytics)
    @GetMapping("/api/books/books-per-genre")
    public ResponseEntity<Map<String, Long>> getBooksPerGenre() {
        return ResponseEntity.ok(bookService.getBooksPerGenre());
    }

    // ==================== REST API ENDPOINTS (/api/v1/books) ====================

    @Operation(summary = "Get all books", description = "Retrieve a paginated list of all books")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Books retrieved successfully"), @ApiResponse(responseCode = "500", description = "Internal server error")})
    @GetMapping("/api/v1/books")
    public ResponseEntity<Map<String, Object>> getAllBooks(@Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page, @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = bookService.getAllBooksDTOPaginated(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get book by ID", description = "Retrieve a specific book by its ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Book found"), @ApiResponse(responseCode = "404", description = "Book not found")})
    @GetMapping("/api/v1/books/{id}")
    public ResponseEntity<BookDTO> getBookById(@Parameter(description = "Book ID") @PathVariable Long id) {
        Optional<BookDTO> book = bookService.getBookByIdDTO(id);
        return book.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/api/v1/books", consumes = "application/json", produces = "application/json")
    public ResponseEntity<BookDTO> createBook(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails, @RequestBody BookDTO bookDTO) {
        System.out.println("🔥 DEBUG: createBook method called with: " + bookDTO);
        try {
            // Get current user
            User currentUser = userService.getUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isAdmin = currentUser.getRole() == User.Role.ROLE_ADMIN;

            // For non-admin users, force them to be the owner
            if (!isAdmin) {
                bookDTO = new BookDTO(
                    bookDTO.id(),
                    bookDTO.title(),
                    bookDTO.author(),
                    bookDTO.genre(),
                    bookDTO.description(),
                    bookDTO.hasCoverImage(),
                    new es.codeurjc13.librored.dto.UserBasicDTO(currentUser.getId(), currentUser.getUsername())
                );
            }

            BookDTO createdBook = bookService.createBookDTO(bookDTO);
            URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(createdBook.id()).toUri();
            System.out.println("🔥 DEBUG: Book created successfully: " + createdBook);
            return ResponseEntity.created(location).body(createdBook);
        } catch (IllegalArgumentException e) {
            System.out.println("🔥 DEBUG: Error creating book: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update book", description = "Update an existing book")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Book updated successfully"), @ApiResponse(responseCode = "404", description = "Book not found"), @ApiResponse(responseCode = "400", description = "Invalid book data")})
    @PutMapping("/api/v1/books/{id}")
    public ResponseEntity<BookDTO> updateBook(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails, @Parameter(description = "Book ID") @PathVariable Long id, @Parameter(description = "Updated book data") @Valid @RequestBody BookDTO bookDTO) {
        try {
            // Get current user
            User currentUser = userService.getUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isAdmin = currentUser.getRole() == User.Role.ROLE_ADMIN;

            // Check if book exists and get current book
            Optional<BookDTO> existingBookOpt = bookService.getBookByIdDTO(id);
            if (existingBookOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            BookDTO existingBook = existingBookOpt.get();

            // Authorization check: Admin can edit any book, User can only edit their own books
            if (!isAdmin && !existingBook.owner().id().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // For non-admin users, prevent them from changing ownership
            if (!isAdmin) {
                bookDTO = new BookDTO(
                    bookDTO.id(),
                    bookDTO.title(),
                    bookDTO.author(),
                    bookDTO.genre(),
                    bookDTO.description(),
                    bookDTO.hasCoverImage(),
                    existingBook.owner() // Keep original owner
                );
            }

            Optional<BookDTO> updatedBook = bookService.updateBookDTO(id, bookDTO);
            return updatedBook.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Delete book", description = "Delete a book by ID")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Book deleted successfully"), @ApiResponse(responseCode = "404", description = "Book not found")})
    @DeleteMapping("/api/v1/books/{id}")
    public ResponseEntity<Void> deleteBook(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails, @Parameter(description = "Book ID") @PathVariable Long id) {
        try {
            // Get current user
            User currentUser = userService.getUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isAdmin = currentUser.getRole() == User.Role.ROLE_ADMIN;

            // Check if book exists
            Optional<BookDTO> existingBookOpt = bookService.getBookByIdDTO(id);
            if (existingBookOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            BookDTO existingBook = existingBookOpt.get();

            // Authorization check: Admin can delete any book, User can only delete their own books
            if (!isAdmin && !existingBook.owner().id().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            boolean deleted = bookService.deleteBookDTO(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get books by owner", description = "Retrieve books owned by a specific user")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Books retrieved successfully"), @ApiResponse(responseCode = "404", description = "Owner not found")})
    @GetMapping("/api/v1/books/owner/{ownerId}")
    public ResponseEntity<List<BookDTO>> getBooksByOwner(@Parameter(description = "Owner ID") @PathVariable Long ownerId) {
        List<BookDTO> books = bookService.getBooksByOwnerIdDTO(ownerId);
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Get available books by owner", description = "Get available books for lending by owner")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Available books retrieved"), @ApiResponse(responseCode = "404", description = "Owner not found")})
    @GetMapping("/api/v1/books/available/{ownerId}")
    public ResponseEntity<List<BookBasicDTO>> getAvailableBooksByOwner(@Parameter(description = "Owner ID") @PathVariable Long ownerId) {
        List<BookBasicDTO> books = bookService.getAvailableBooksByOwnerIdDTO(ownerId);
        return ResponseEntity.ok(books);
    }

    @Operation(summary = "Get book recommendations", description = "Get book recommendations for a user")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Recommendations retrieved"), @ApiResponse(responseCode = "404", description = "User not found")})
    @GetMapping("/api/v1/books/recommendations/{userId}")
    public ResponseEntity<List<BookDTO>> getRecommendations(@Parameter(description = "User ID") @PathVariable Long userId) {
        List<BookDTO> recommendations = bookService.getRecommendationsForUserDTO(userId);
        return ResponseEntity.ok(recommendations);
    }

    @Operation(summary = "Get books per genre statistics", description = "Get count of books grouped by genre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")})
    @GetMapping("/api/v1/books/stats/genre")
    public ResponseEntity<Map<String, Long>> getBooksPerGenreStats() {
        Map<String, Long> stats = bookService.getBooksPerGenreDTO();
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "Get book cover image", description = "Download the cover image for a specific book")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Cover image retrieved successfully"), @ApiResponse(responseCode = "404", description = "Book or cover image not found")})
    @GetMapping("/api/v1/books/{id}/cover")
    public ResponseEntity<Resource> getBookCoverImage(@Parameter(description = "Book ID") @PathVariable Long id) {
        try {
            Book book = bookService.findBookById(id);

            if (book.getCoverPic() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Resource file = new InputStreamResource(book.getCoverPic().getBinaryStream());
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "image/jpeg").header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"book-" + id + "-cover.jpg\"").body(file);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Upload book cover image", description = "Upload a cover image for a specific book")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Cover image uploaded successfully"), @ApiResponse(responseCode = "400", description = "Invalid file or request"), @ApiResponse(responseCode = "404", description = "Book not found"), @ApiResponse(responseCode = "413", description = "File too large"), @ApiResponse(responseCode = "415", description = "Unsupported media type")})
    @PostMapping("/api/v1/books/{id}/cover")
    public ResponseEntity<Map<String, String>> uploadBookCoverImage(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails, @Parameter(description = "Book ID") @PathVariable Long id, @Parameter(description = "Cover image file") @RequestParam("file") MultipartFile file) {

        Map<String, String> response = new HashMap<>();

        try {
            // Get current user
            User currentUser = userService.getUserByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isAdmin = currentUser.getRole() == User.Role.ROLE_ADMIN;
            // Validate file
            if (file.isEmpty()) {
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Check file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                response.put("error", "File size exceeds maximum limit of 5MB");
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
            }

            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/"))) {
                response.put("error", "File must be an image");
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
            }

            // Check if book exists and get book details
            Optional<BookDTO> bookDTOOptional = bookService.getBookByIdDTO(id);
            if (bookDTOOptional.isEmpty()) {
                response.put("error", "Book not found");
                return ResponseEntity.notFound().build();
            }

            BookDTO bookDTO = bookDTOOptional.get();

            // Authorization check: Admin can upload to any book, User can only upload to their own books
            if (!isAdmin && !bookDTO.owner().id().equals(currentUser.getId())) {
                response.put("error", "You can only upload cover images to your own books");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Upload the image
            bookService.uploadBookCover(id, file);

            response.put("message", "Cover image uploaded successfully");
            response.put("bookId", id.toString());
            response.put("fileName", file.getOriginalFilename());
            response.put("size", String.valueOf(file.getSize()));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("error", "Failed to upload cover image: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
