package es.codeurjc13.librored.dto;

import es.codeurjc13.librored.model.Book;

/**
 * Book DTO for REST API operations
 * Contains all book information including owner reference
 */
public record BookDTO(
        Long id,
        String title,
        String author,
        Book.Genre genre,
        String description,
        boolean hasCoverImage,
        UserBasicDTO owner) {
}