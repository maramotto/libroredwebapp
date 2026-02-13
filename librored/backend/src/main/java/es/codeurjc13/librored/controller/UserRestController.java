package es.codeurjc13.librored.controller;

import es.codeurjc13.librored.dto.UserDTO;
import es.codeurjc13.librored.dto.UserBasicDTO;
import es.codeurjc13.librored.model.User;
import es.codeurjc13.librored.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class UpdateUsernameRequest {
    public String username;
}

class VerifyPasswordRequest {
    public String password;
}

class UpdatePasswordRequest {
    public String currentPassword;
    public String newPassword;
}

@RestController
@Tag(name = "Users", description = "User management API")
public class UserRestController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserRestController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/api/users/update-username")
    public ResponseEntity<Map<String, Object>> updateUsername(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            @RequestBody UpdateUsernameRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "error", "User not authenticated."));
        }

        String email = userDetails.getUsername();  // Always fetch by email
        Optional<User> user = userService.getUserByEmail(email);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", "User not found."));
        }

        user.get().setUsername(request.username);
        userService.saveUser(user.get());

        //  Refresh authentication to ensure future requests still work
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.get(), user.get().getEncodedPassword(), userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return ResponseEntity.ok(Map.of("success", true, "message", "Username updated successfully!"));
    }


    @PostMapping("/api/users/update-password")
    public ResponseEntity<Map<String, Object>> updatePassword(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            @RequestBody UpdatePasswordRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "error", "User not authenticated."));
        }

        String email = userDetails.getUsername(); //  Always fetch by email
        Optional<User> user = userService.getUserByEmail(email);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", "User not found."));
        }

        if (!passwordEncoder.matches(request.currentPassword, user.get().getEncodedPassword())) {
            return ResponseEntity.ok(Map.of("success", false, "error", "Incorrect current password."));
        }

        // Encode and update the new password
        user.get().setEncodedPassword(passwordEncoder.encode(request.newPassword));
        userService.saveUser(user.get());

        // Invalidate session after password change
        SecurityContextHolder.clearContext();

        // Redirect to login page with success message
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password updated successfully! Redirecting to login..."
        ));
    }


    @PostMapping("/api/users/verify-password")
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails,
            @RequestBody VerifyPasswordRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "error", "User is not authenticated."));
        }

        String email = userDetails.getUsername(); // Always fetch by email, NOT username
        Optional<User> user = userService.getUserByEmail(email);

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "error", "User not found."));
        }

        if (!passwordEncoder.matches(request.password, user.get().getEncodedPassword())) {
            return ResponseEntity.ok(Map.of("success", false, "error", "Incorrect current password."));
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Password verified! You can now enter a new password."));
    }

    // ==================== AUTHENTICATION ENDPOINT (/api/users/me) ====================

    @Operation(summary = "Get current user", description = "Get currently authenticated user information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/api/users/me")
    public ResponseEntity<UserDTO> getCurrentUser(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = userDetails.getUsername();
        Optional<UserDTO> user = userService.getUserByEmailDTO(email);

        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    // ==================== REST API ENDPOINTS (/api/v1/users) ====================

    @Operation(summary = "Get all users", description = "Retrieve a paginated list of all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/api/v1/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = userService.getAllUsersDTOPaginated(page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/api/v1/users/{id}")
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        Optional<UserDTO> user = userService.getUserByIdDTO(id);
        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new user", description = "Create a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid user data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/api/v1/users")
    public ResponseEntity<UserDTO> createUser(
            @Parameter(description = "User data") @Valid @RequestBody UserDTO userDTO) {
        try {
            UserDTO createdUser = userService.createUserDTO(userDTO);
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(createdUser.id())
                    .toUri();
            return ResponseEntity.created(location).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Update user", description = "Update an existing user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid user data")
    })
    @PutMapping("/api/v1/users/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Parameter(description = "Updated user data") @Valid @RequestBody UserDTO userDTO) {
        Optional<UserDTO> updatedUser = userService.updateUserDTO(id, userDTO);
        return updatedUser.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete user", description = "Delete a user by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/api/v1/users/{id}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User loggedUser,
            HttpServletRequest request) {
        
        // Check if the admin is trying to delete their own account
        boolean isDeletingSelf = false;
        if (loggedUser != null) {
            Optional<User> userToDelete = userService.getUserById(id);
            if (userToDelete.isPresent()) {
                // Compare logged user's email with user-to-delete's email
                isDeletingSelf = userToDelete.get().getEmail().equals(loggedUser.getUsername());
            }
        }
        
        // Delete the user
        boolean deleted = userService.deleteUserDTO(id);
        
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        // If admin deleted themselves, clear security context and return special response
        if (isDeletingSelf) {
            SecurityContextHolder.clearContext();
            // Invalidate the session
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
            return ResponseEntity.ok(Map.of(
                "deleted", true,
                "selfDeletion", true,
                "message", "Account deleted successfully. You have been logged out."
            ));
        }
        
        return ResponseEntity.ok(Map.of(
            "deleted", true,
            "selfDeletion", false,
            "message", "User deleted successfully."
        ));
    }

    @Operation(summary = "Get user by username", description = "Retrieve a user by their username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/api/v1/users/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(
            @Parameter(description = "Username") @PathVariable String username) {
        Optional<UserDTO> user = userService.getUserByUsernameDTO(username);
        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get user by email", description = "Retrieve a user by their email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/api/v1/users/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(
            @Parameter(description = "Email") @PathVariable String email) {
        Optional<UserDTO> user = userService.getUserByEmailDTO(email);
        return user.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get valid borrowers", description = "Get list of valid borrowers for a specific lender")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Valid borrowers retrieved"),
            @ApiResponse(responseCode = "404", description = "Lender not found")
    })
    @GetMapping("/api/v1/users/{lenderId}/valid-borrowers")
    public ResponseEntity<List<UserBasicDTO>> getValidBorrowers(
            @Parameter(description = "Lender ID") @PathVariable Long lenderId) {
        List<UserBasicDTO> borrowers = userService.getValidBorrowersDTO(lenderId);
        return ResponseEntity.ok(borrowers);
    }


}
