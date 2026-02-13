package es.codeurjc13.librored.service;

import es.codeurjc13.librored.dto.BookDTO;
import es.codeurjc13.librored.dto.BookBasicDTO;
import es.codeurjc13.librored.mapper.BookMapper;
import es.codeurjc13.librored.model.Book;
import es.codeurjc13.librored.model.User;
import es.codeurjc13.librored.repository.BookRepository;
import es.codeurjc13.librored.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Blob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookMapper bookMapper;

    public BookService(BookRepository bookRepository, UserRepository userRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.bookMapper = bookMapper;
    }

    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Book> getBooks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return bookRepository.findAll(pageable);
    }

    // for the api endpoint @GetMapping("/{id}/cover")
    public Book findBookById(Long id) {
        Optional<Book> bookOptional = bookRepository.findById(id);
        return bookOptional.orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    @Transactional
    public void saveBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null");
        }
        bookRepository.save(book);
    }

    public Map<String, Long> getBooksPerGenre() {
        List<Object[]> results = bookRepository.countBooksByGenre();
        Map<String, Long> booksPerGenre = new HashMap<>();

        for (Object[] result : results) {
            Book.Genre genreEnum = (Book.Genre) result[0]; // Cast Enum correctly
            String genre = genreEnum.name(); // Convert Enum to String

            Long count = (Long) result[1];
            booksPerGenre.put(genre, count);
        }

        return booksPerGenre;
    }

    public List<Book> getAvailableBooksByOwnerId(Long ownerId) {
        return bookRepository.findAvailableBooksByOwnerId(ownerId);
    }

    @Transactional
    public void deleteBook(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Book ID cannot be null");
        }
        bookRepository.deleteById(id);
    }

    public List<Book> getBooksByOwner(User owner) {
        return bookRepository.findByOwner(owner);
    }

    public List<Book> getRecommendationsForUser(Long userId) {
        return bookRepository.findRecommendedBooks(userId);
    }

    // ==================== DTO-BASED METHODS FOR REST API ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getAllBooksDTOPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findAll(pageable);
        
        return createPaginationResponse(bookMapper.toDTOs(bookPage.getContent()), bookPage);
    }

    private Map<String, Object> createPaginationResponse(List<BookDTO> content, Page<?> page) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("currentPage", page.getNumber());
        response.put("totalPages", page.getTotalPages());
        response.put("totalItems", page.getTotalElements());
        response.put("hasNext", page.hasNext());
        response.put("hasPrevious", page.hasPrevious());
        response.put("isFirst", page.isFirst());
        response.put("isLast", page.isLast());
        return response;
    }

    public Optional<BookDTO> getBookByIdDTO(Long id) {
        Optional<Book> book = bookRepository.findById(id);
        return book.map(bookMapper::toDTO);
    }

    public BookDTO createBookDTO(BookDTO bookDTO) {
        System.out.println("🔥 DEBUG: BookService.createBookDTO called with: " + bookDTO);
        try {
            Book book = bookMapper.toDomain(bookDTO);
            System.out.println("🔥 DEBUG: Book entity created from DTO: " + book);

            // Set the owner from the DTO
            if (bookDTO.owner() != null) {
                System.out.println("🔥 DEBUG: Looking for owner with id: " + bookDTO.owner().id());
                User owner = userRepository.findById(bookDTO.owner().id())
                        .orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + bookDTO.owner().id()));
                book.setOwner(owner);
                System.out.println("🔥 DEBUG: Owner set successfully: " + owner.getUsername());
            }

            System.out.println("🔥 DEBUG: About to save book to repository...");
            Book savedBook = bookRepository.save(book);
            System.out.println("🔥 DEBUG: Book saved successfully with id: " + savedBook.getId());
            return bookMapper.toDTO(savedBook);
        } catch (Exception e) {
            System.out.println("🔥 DEBUG: Exception in createBookDTO: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Optional<BookDTO> updateBookDTO(Long id, BookDTO bookDTO) {
        Optional<Book> existingBookOpt = bookRepository.findById(id);
        if (existingBookOpt.isPresent()) {
            Book book = existingBookOpt.get();
            book.setTitle(bookDTO.title());
            book.setAuthor(bookDTO.author());
            book.setGenre(bookDTO.genre());
            book.setDescription(bookDTO.description());
            
            // Update owner if provided
            if (bookDTO.owner() != null) {
                User owner = userRepository.findById(bookDTO.owner().id())
                        .orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + bookDTO.owner().id()));
                book.setOwner(owner);
            }
            
            Book savedBook = bookRepository.save(book);
            return Optional.of(bookMapper.toDTO(savedBook));
        }
        return Optional.empty();
    }

    public boolean deleteBookDTO(Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<BookDTO> getBooksByOwnerIdDTO(Long ownerId) {
        return userRepository.findById(ownerId)
                .map(owner -> bookMapper.toDTOs(bookRepository.findByOwner(owner)))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<BookBasicDTO> getAvailableBooksByOwnerIdDTO(Long ownerId) {
        List<Book> books = bookRepository.findAvailableBooksByOwnerId(ownerId);
        return bookMapper.toBasicDTOs(books);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getBooksPerGenreDTO() {
        return getBooksPerGenre(); // Reuse existing method to avoid duplication
    }

    @Transactional(readOnly = true)
    public List<BookDTO> getRecommendationsForUserDTO(Long userId) {
        List<Book> books = bookRepository.findRecommendedBooks(userId);
        return bookMapper.toDTOs(books);
    }

    // Upload a cover image for a book
    @Transactional
    public void uploadBookCover(Long bookId, MultipartFile file) throws IOException {
        Optional<Book> bookOptional = bookRepository.findById(bookId);
        if (bookOptional.isEmpty()) {
            throw new IllegalArgumentException("Book not found with id: " + bookId);
        }

        Book book = bookOptional.get();
        
        try {
            // Convert MultipartFile to Blob
            byte[] imageBytes = file.getBytes();
            Blob imageBlob = new javax.sql.rowset.serial.SerialBlob(imageBytes);
            
            book.setCoverPic(imageBlob);
            bookRepository.save(book);
            
        } catch (Exception e) {
            throw new IOException("Failed to process image file: " + e.getMessage(), e);
        }
    }

}
