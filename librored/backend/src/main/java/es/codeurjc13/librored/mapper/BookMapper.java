package es.codeurjc13.librored.mapper;

import es.codeurjc13.librored.dto.BookBasicDTO;
import es.codeurjc13.librored.dto.BookDTO;
import es.codeurjc13.librored.model.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.List;

/**
 * Mapper for converting between Book entities and DTOs
 */
@Component
public class BookMapper {

    @Autowired
    private UserMapper userMapper;

    /**
     * Convert Book entity to BookDTO
     */
    public BookDTO toDTO(Book book) {
        if (book == null) {
            return null;
        }
        return new BookDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getDescription(),
                book.getCoverPic() != null,
                book.getOwner() != null ? userMapper.toBasicDTO(book.getOwner()) : null
        );
    }

    /**
     * Convert Book entity to BookBasicDTO
     */
    public BookBasicDTO toBasicDTO(Book book) {
        if (book == null) {
            return null;
        }
        return new BookBasicDTO(
                book.getId(),
                book.getTitle(),
                book.getAuthor()
        );
    }

    /**
     * Convert BookDTO to Book entity
     */
    public Book toDomain(BookDTO bookDTO) {
        if (bookDTO == null) {
            return null;
        }
        Book book = new Book();
        // Only set ID if it's not null and not 0 (for new entities, let Hibernate generate the ID)
        if (bookDTO.id() != null && bookDTO.id() > 0) {
            book.setId(bookDTO.id());
        }
        book.setTitle(bookDTO.title());
        book.setAuthor(bookDTO.author());
        book.setGenre(bookDTO.genre());
        book.setDescription(bookDTO.description());
        // Note: Owner and coverPic are typically set separately in services
        return book;
    }

    /**
     * Convert collection of Book entities to DTOs
     */
    public List<BookDTO> toDTOs(Collection<Book> books) {
        return books.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Convert collection of Book entities to Basic DTOs
     */
    public List<BookBasicDTO> toBasicDTOs(Collection<Book> books) {
        return books.stream()
                .map(this::toBasicDTO)
                .toList();
    }
}