package es.codeurjc13.librored.dto;

import es.codeurjc13.librored.model.User;

/**
 * Complete User DTO for REST API operations
 * Contains all user information except sensitive data (password)
 */
public record UserDTO(
        Long id,
        String username,
        String email,
        User.Role role) {
}