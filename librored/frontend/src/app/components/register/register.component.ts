import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  username = '';
  email = '';
  password = '';
  confirmPassword = '';
  encodedPassword = '';
  isSubmitting = false;
  showErrorModal = false;
  showSuccessModal = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  register() {
    // Validate form
    if (!this.username || !this.email || !this.encodedPassword) {
      this.showError('All fields are required');
      return;
    }

    if (this.encodedPassword !== this.confirmPassword) {
      this.showError('Passwords do not match');
      return;
    }

    if (this.encodedPassword.length < 6) {
      this.showError('Password must be at least 6 characters long');
      return;
    }

    this.isSubmitting = true;

    this.authService.register(this.username, this.email, this.encodedPassword).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        if (response.status === 'SUCCESS') {
          this.showSuccess('Registration successful! You can now log in.');
        } else {
          this.showError(response.message || 'Registration failed');
        }
      },
      error: (error) => {
        this.isSubmitting = false;
        this.showError('Registration failed: ' + (error.error?.message || error.message || 'Unknown error'));
      }
    });
  }

  onSubmit() {
    this.register();
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }

  private showError(message: string) {
    this.errorMessage = message;
    this.showErrorModal = true;
  }

  private showSuccess(message: string) {
    this.successMessage = message;
    this.showSuccessModal = true;
  }

  closeErrorModal() {
    this.showErrorModal = false;
  }

  closeSuccessModal() {
    this.showSuccessModal = false;
    // Navigate to login after successful registration
    this.router.navigate(['/login']);
  }
}