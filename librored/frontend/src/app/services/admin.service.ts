import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserDTO {
  id: number;
  username: string;
  email: string;
  password?: string; // Optional for updates
  role: 'ROLE_USER' | 'ROLE_ADMIN';
}

export interface UserBasicDTO {
  id: number;
  username: string;
  email: string;
}

export interface PaginatedUsersResponse {
  content: UserDTO[];
  currentPage: number;
  totalPages: number;
  totalItems: number;
  hasNext: boolean;
  hasPrevious: boolean;
  isFirst: boolean;
  isLast: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly API_URL = '/api/v1/users';

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

  /**
   * Get all users with pagination
   */
  getAllUsers(page: number = 0, size: number = 10): Observable<PaginatedUsersResponse> {
    return this.http.get<PaginatedUsersResponse>(
      `${this.API_URL}?page=${page}&size=${size}`,
      { headers: this.getAuthHeaders(), withCredentials: true }
    );
  }

  /**
   * Get user by ID
   */
  getUserById(id: number): Observable<UserDTO> {
    return this.http.get<UserDTO>(
      `${this.API_URL}/${id}`,
      { headers: this.getAuthHeaders(), withCredentials: true }
    );
  }

  /**
   * Create new user
   */
  createUser(user: Omit<UserDTO, 'id'>): Observable<UserDTO> {
    return this.http.post<UserDTO>(
      this.API_URL,
      user,
      { headers: this.getAuthHeaders(), withCredentials: true }
    );
  }

  /**
   * Update existing user
   */
  updateUser(id: number, user: Omit<UserDTO, 'id'>): Observable<UserDTO> {
    return this.http.put<UserDTO>(
      `${this.API_URL}/${id}`,
      user,
      { headers: this.getAuthHeaders(), withCredentials: true }
    );
  }

  /**
   * Delete user by ID
   */
  deleteUser(id: number): Observable<any> {
    return this.http.delete(
      `${this.API_URL}/${id}`,
      { headers: this.getAuthHeaders(), withCredentials: true }
    );
  }

  /**
   * Get user by username
   */
  getUserByUsername(username: string): Observable<UserDTO> {
    return this.http.get<UserDTO>(
      `${this.API_URL}/username/${username}`,
      { headers: this.getAuthHeaders(), withCredentials: true }
    );
  }

  /**
   * Get user by email
   */
  getUserByEmail(email: string): Observable<UserDTO> {
    return this.http.get<UserDTO>(
      `${this.API_URL}/email/${email}`,
      { headers: this.getAuthHeaders(), withCredentials: true }
    );
  }

  /**
   * Download admin report (PDF)
   */
  downloadAdminReport(): Observable<Blob> {
    console.log('Starting admin report download...');
    const token = localStorage.getItem('access_token');

    if (!token) {
      throw new Error('No access token available. Please login first.');
    }

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/pdf'
    });

    return this.http.get('https://localhost:8443/api/download-report', {
      responseType: 'blob',
      headers: headers
    });
  }
}