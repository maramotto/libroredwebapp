package es.codeurjc13.librored.dto;

/**
 * Basic Book DTO for references in other entities
 * Used when we only need minimal book information (e.g., in loans)
 */
public record BookBasicDTO(
        Long id,
        String title,
        String author) {
}