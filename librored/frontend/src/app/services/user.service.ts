import { Injectable } from "@angular/core";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Observable, throwError } from "rxjs";
import { catchError, map } from "rxjs/operators";
import { UserDTO } from "../dtos/user.dto";

@Injectable({ providedIn: "root" })
export class UserService {
  private readonly API_URL = "/api/users";
  private readonly API_V1_URL = "/api/v1/users";

  constructor(private http: HttpClient) {}

  /**
   * Get HTTP headers with JWT token
   */
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('access_token');
    if (token) {
      return new HttpHeaders({
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      });
    }
    return new HttpHeaders({
      'Content-Type': 'application/json'
    });
  }

  // Get all users (for admin or selection dropdowns)
  getUsers(): Observable<UserDTO[]> {
    return this.http.get<{content: UserDTO[]}>(`${this.API_V1_URL}?page=0&size=1000`, {
      headers: this.getAuthHeaders(),
      withCredentials: true
    })
      .pipe(
        map(response => response.content),
        catchError(this.handleError)
      );
  }

  // Get single user by ID
  getUser(id: number): Observable<UserDTO> {
    return this.http.get<UserDTO>(`${this.API_V1_URL}/${id}`, {
      headers: this.getAuthHeaders(),
      withCredentials: true
    })
      .pipe(catchError(this.handleError));
  }

  // Get current user profile
  getCurrentUser(): Observable<UserDTO> {
    return this.http.get<UserDTO>(`${this.API_URL}/me`, {
      headers: this.getAuthHeaders(),
      withCredentials: true
    })
      .pipe(catchError(this.handleError));
  }

  private handleError(error: any): Observable<never> {
    console.error('UserService error:', error);
    let errorMessage = 'An error occurred';
    
    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    } else if (typeof error.error === 'string') {
      errorMessage = error.error;
    }
    
    return throwError(() => new Error(errorMessage));
  }
}