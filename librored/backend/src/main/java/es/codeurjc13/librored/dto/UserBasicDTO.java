package es.codeurjc13.librored.dto;

/**
 * Basic User DTO for references in other entities
 * Used when we only need minimal user information (e.g., in loans)
 */
public record UserBasicDTO(
        Long id,
        String username) {
}