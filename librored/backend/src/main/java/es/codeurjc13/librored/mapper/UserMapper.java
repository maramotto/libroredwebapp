package es.codeurjc13.librored.mapper;

import es.codeurjc13.librored.dto.UserBasicDTO;
import es.codeurjc13.librored.dto.UserDTO;
import es.codeurjc13.librored.model.User;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.List;

/**
 * Mapper for converting between User entities and DTOs
 */
@Component
public class UserMapper {

    /**
     * Convert User entity to UserDTO
     */
    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }

    /**
     * Convert User entity to UserBasicDTO
     */
    public UserBasicDTO toBasicDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserBasicDTO(
                user.getId(),
                user.getUsername()
        );
    }

    /**
     * Convert UserDTO to User entity
     * Note: Password is not included in DTO for security reasons
     */
    public User toDomain(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        User user = new User();
        user.setId(userDTO.id());
        user.setUsername(userDTO.username());
        user.setEmail(userDTO.email());
        user.setRole(userDTO.role());
        return user;
    }

    /**
     * Convert collection of User entities to DTOs
     */
    public List<UserDTO> toDTOs(Collection<User> users) {
        return users.stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Convert collection of User entities to Basic DTOs
     */
    public List<UserBasicDTO> toBasicDTOs(Collection<User> users) {
        return users.stream()
                .map(this::toBasicDTO)
                .toList();
    }
}