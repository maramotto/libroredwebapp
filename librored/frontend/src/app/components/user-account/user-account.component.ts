import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserAccountService } from '../../services/user-account.service';
import { BookService } from '../../services/book.service';

export interface UserInfo {
  id: number;
  username: string;
  email: string;
  role: string;
}

export interface BookRecommendation {
  id: number;
  title: string;
  author: string;
  genre: string;
  description?: string;
  hasCoverImage: boolean;
  owner: {
    id: number;
    username: string;
    email: string;
  };
}

@Component({
  selector: 'app-user-account',
  templateUrl: './user-account.component.html',
  styleUrls: ['./user-account.component.css']
})
export class UserAccountComponent implements OnInit {
  user: UserInfo | null = null;
  recommendations: BookRecommendation[] = [];

  // Username change
  newUsername: string = '';

  // Password change
  currentPassword: string = '';
  newPassword: string = '';
  confirmPassword: string = '';
  passwordVerified: boolean = false;

  // Loading states
  loading: boolean = false;
  loadingRecommendations: boolean = false;

  // Messages
  successMessage: string = '';
  errorMessage: string = '';

  constructor(
    private authService: AuthService,
    private userAccountService: UserAccountService,
    private bookService: BookService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUserInfo();
  }

  loadUserInfo(): void {
    this.loading = true;
    this.userAccountService.getCurrentUser().subscribe({
      next: (user) => {
        this.user = user;
        this.newUsername = user.username; // Pre-fill current username
        this.loading = false;
        // Load recommendations after user is loaded
        this.loadRecommendations();
      },
      error: (error) => {
        console.error('Error loading user info:', error);
        this.errorMessage = 'Failed to load user information';
        this.loading = false;
        // If unauthorized, redirect to login
        if (error.status === 401) {
          this.router.navigate(['/login']);
        }
      }
    });
  }

  loadRecommendations(): void {
    if (!this.user?.id) return;

    this.loadingRecommendations = true;
    this.userAccountService.getRecommendations(this.user.id).subscribe({
      next: (recommendations) => {
        this.recommendations = recommendations;
        this.loadingRecommendations = false;
      },
      error: (error) => {
        console.error('Error loading recommendations:', error);
        this.loadingRecommendations = false;
      }
    });
  }

  updateUsername(): void {
    if (!this.newUsername || this.newUsername.trim() === '') {
      this.errorMessage = 'Username cannot be empty';
      return;
    }

    if (this.newUsername === this.user?.username) {
      this.errorMessage = 'New username must be different from current username';
      return;
    }

    this.loading = true;
    this.clearMessages();

    this.userAccountService.updateUsername(this.newUsername).subscribe({
      next: (response) => {
        this.successMessage = 'Username updated successfully!';
        if (this.user) {
          this.user.username = this.newUsername;
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error updating username:', error);
        this.errorMessage = error.error?.error || 'Failed to update username';
        this.loading = false;
      }
    });
  }

  verifyPassword(): void {
    if (!this.currentPassword) {
      this.errorMessage = 'Please enter your current password';
      return;
    }

    this.loading = true;
    this.clearMessages();

    this.userAccountService.verifyPassword(this.currentPassword).subscribe({
      next: (response) => {
        this.successMessage = 'Password verified! You can now enter a new password.';
        this.passwordVerified = true;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error verifying password:', error);
        this.errorMessage = error.error?.error || 'Incorrect current password';
        this.passwordVerified = false;
        this.loading = false;
      }
    });
  }

  updatePassword(): void {
    if (!this.passwordVerified) {
      this.errorMessage = 'Please verify your current password first';
      return;
    }

    if (!this.newPassword || this.newPassword.length < 4) {
      this.errorMessage = 'New password must be at least 4 characters long';
      return;
    }

    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'New passwords do not match';
      return;
    }

    this.loading = true;
    this.clearMessages();

    this.userAccountService.updatePassword(this.currentPassword, this.newPassword).subscribe({
      next: (response) => {
        this.successMessage = 'Password updated successfully! Redirecting to login...';
        // Clear form
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.passwordVerified = false;

        // Logout and redirect to login after delay
        setTimeout(() => {
          this.authService.logOut();
          this.router.navigate(['/login'], {
            queryParams: { message: 'Password updated successfully. Please log in again.' }
          });
        }, 2000);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error updating password:', error);
        this.errorMessage = error.error?.error || 'Failed to update password';
        this.loading = false;
      }
    });
  }

  resetPasswordForm(): void {
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.passwordVerified = false;
    this.clearMessages();
  }

  clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }
}