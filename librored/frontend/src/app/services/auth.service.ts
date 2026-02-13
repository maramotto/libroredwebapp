import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { of } from 'rxjs';

const BASE_URL = "/api/auth";

interface AuthResponse {
  status: 'SUCCESS' | 'FAILURE';
  message: string;
  error?: string;
  accessToken?: string;
  refreshToken?: string;
}

interface LoginUser {
  username: string;
  email: string;
  roles?: string[];
  id?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  public logged: boolean = false;
  public user: LoginUser | undefined;

  // BehaviorSubject to notify components about auth state changes
  private authStateSubject = new BehaviorSubject<boolean>(false);
  public authState$ = this.authStateSubject.asObservable();

  private readonly ACCESS_TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly USER_KEY = 'current_user';

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    console.log('🚀 AuthService constructor - Checking JWT token');
    this.initializeFromStorage();
  }

  /**
   * Initialize auth state from localStorage
   */
  private initializeFromStorage(): void {
    const accessToken = localStorage.getItem(this.ACCESS_TOKEN_KEY);
    const userStr = localStorage.getItem(this.USER_KEY);

    if (accessToken && userStr) {
      try {
        this.user = JSON.parse(userStr);
        this.logged = true;
        this.authStateSubject.next(true);
        console.log('🔐 User restored from localStorage:', this.user);
      } catch (error) {
        console.error('Error parsing user from localStorage:', error);
        this.clearTokens();
      }
    }
  }

  /**
   * Login with username and password (JWT version)
   */
  public logIn(user: string, pass: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      BASE_URL + "/login",
      { username: user, password: pass }
    ).pipe(
      tap((response: AuthResponse) => {
        if (response.status === 'SUCCESS' && response.accessToken) {
          this.storeTokens(response.accessToken, response.refreshToken || '');
          this.setUserLoggedIn(user);
        }
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Register new user
   */
  public register(username: string, email: string, encodedPassword: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      BASE_URL + "/register",
      { username, email, encodedPassword }
    ).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Logout current user (JWT version)
   */
  public logOut(): void {
    // Clear tokens and user state
    this.clearTokens();

    // Optional: Call backend logout endpoint if needed
    this.http.post(BASE_URL + "/logout", {}).subscribe({
      next: (_) => console.log("Backend logout successful"),
      error: (error) => console.warn("Backend logout failed:", error)
    });

    this.router.navigate(['/']);
  }

  /**
   * Check if user is currently logged in
   */
  public isLogged(): boolean {
    return this.logged;
  }

  /**
   * Check if user is currently logged in (compatibility method)
   */
  public isLoggedIn(): boolean {
    return this.logged;
  }

  /**
   * Check if current user is admin (professor's pattern)
   */
  public isAdmin(): boolean {
    if (!this.user || !this.user.roles) {
      return false;
    }
    // Check for different role formats: ADMIN, ROLE_ADMIN, or authority objects
    return this.user.roles.some(role => {
      if (typeof role === 'string') {
        return role === "ADMIN" || role === "ROLE_ADMIN" || role.includes("ADMIN");
      }
      // Handle authority objects
      return false;
    });
  }

  /**
   * Get current user
   */
  currentUser(): LoginUser | undefined {
    return this.user;
  }

  /**
   * Store JWT tokens in localStorage
   */
  private storeTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(this.ACCESS_TOKEN_KEY, accessToken);
    if (refreshToken) {
      localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    }
  }

  /**
   * Get access token from localStorage
   */
  public getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  /**
   * Get refresh token from localStorage
   */
  public getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Clear all tokens and user data
   */
  private clearTokens(): void {
    localStorage.removeItem(this.ACCESS_TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.logged = false;
    this.user = undefined;
    this.authStateSubject.next(false);
  }

  /**
   * Set user as logged in and store user data
   */
  private setUserLoggedIn(username: string): void {
    // Parse JWT payload to get user info (basic implementation)
    const token = this.getAccessToken();
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        // Extract roles from authorities (Spring Security format)
        let roles: string[] = [];
        if (payload.roles && Array.isArray(payload.roles)) {
          roles = payload.roles.map((authority: any) =>
            typeof authority === 'string' ? authority : authority.authority
          );
        }

        this.user = {
          username: payload.sub || username,
          email: payload.email || '',
          roles: roles,
          id: payload.id || payload.userId || undefined
        };
      } catch (error) {
        // Fallback if token parsing fails
        this.user = {
          username: username,
          email: '',
          roles: []
        };
      }

      localStorage.setItem(this.USER_KEY, JSON.stringify(this.user));
      this.logged = true;
      this.authStateSubject.next(true);
    }
  }

  /**
   * Refresh access token using refresh token
   */
  public refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.clearTokens();
      return of({ status: 'FAILURE' as const, message: 'No refresh token available' } as AuthResponse);
    }

    return this.http.post<AuthResponse>(
      BASE_URL + "/refresh",
      { refreshToken }
    ).pipe(
      tap((response: AuthResponse) => {
        if (response.status === 'SUCCESS' && response.accessToken) {
          localStorage.setItem(this.ACCESS_TOKEN_KEY, response.accessToken);
        }
      }),
      catchError((error) => {
        console.error('Token refresh failed:', error);
        this.clearTokens();
        return of({ status: 'FAILURE' as const, message: 'Token refresh failed' } as AuthResponse);
      })
    );
  }

  /**
   * Clear all authentication data (public method for debugging)
   */
  public clearAllAuthData(): void {
    this.clearTokens();
  }

  /**
   * Error handler for HTTP requests
   */
  private handleError = (error: HttpErrorResponse): Observable<AuthResponse> => {
    console.error('Auth service error:', error);
    return of({
      status: 'FAILURE',
      message: error.error?.message || 'Authentication failed',
      error: error.message
    });
  }

}