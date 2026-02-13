package es.codeurjc13.librored.controller.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.codeurjc13.librored.model.User;
import es.codeurjc13.librored.security.jwt.AuthResponse;
import es.codeurjc13.librored.security.jwt.AuthResponse.Status;
import es.codeurjc13.librored.security.jwt.LoginRequest;
import es.codeurjc13.librored.security.jwt.UserLoginService;
import es.codeurjc13.librored.service.UserService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
	
	@Autowired
	private UserLoginService userService;

	@Autowired
	private UserService userManagementService;

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(
			@RequestBody LoginRequest loginRequest,
			HttpServletResponse response) {
		
		return userService.login(response, loginRequest);
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refreshToken(
			@CookieValue(name = "RefreshToken", required = false) String refreshToken, HttpServletResponse response) {

		return userService.refresh(response, refreshToken);
	}

	@PostMapping("/logout")
	public ResponseEntity<AuthResponse> logOut(HttpServletResponse response) {
		return ResponseEntity.ok(new AuthResponse(Status.SUCCESS, userService.logout(response)));
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest) {
		try {
			// Create new user
			User newUser = new User();
			newUser.setUsername(registerRequest.getUsername());
			newUser.setEmail(registerRequest.getEmail());
			newUser.setEncodedPassword(registerRequest.getEncodedPassword());
			newUser.setRole(User.Role.ROLE_USER); // Default role

			// Register the user using existing service
			userManagementService.registerUser(newUser);

			return ResponseEntity.ok(new AuthResponse(Status.SUCCESS, "User registered successfully"));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(new AuthResponse(Status.FAILURE, e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(new AuthResponse(Status.FAILURE, "Registration failed"));
		}
	}

	// Inner class for register request
	public static class RegisterRequest {
		private String username;
		private String email;
		private String encodedPassword;

		// Getters and setters
		public String getUsername() { return username; }
		public void setUsername(String username) { this.username = username; }

		public String getEmail() { return email; }
		public void setEmail(String email) { this.email = email; }

		public String getEncodedPassword() { return encodedPassword; }
		public void setEncodedPassword(String encodedPassword) { this.encodedPassword = encodedPassword; }
	}
}