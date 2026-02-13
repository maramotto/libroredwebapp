import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';

interface UserInfo {
  id: number;
  username: string;
  email: string;
  role: string;
}

interface BookRecommendation {
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


@Injectable({
  providedIn: 'root'
})
export class UserAccountService {
  private readonly BASE_URL = 'https://localhost:8443';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getHeaders(): HttpHeaders {
    const token = this.authService.getAccessToken();
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  getCurrentUser(): Observable<UserInfo> {
    return this.http.get<UserInfo>(`${this.BASE_URL}/api/users/me`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  getRecommendations(userId: number): Observable<BookRecommendation[]> {
    return this.http.get<BookRecommendation[]>(`${this.BASE_URL}/api/v1/books/recommendations/${userId}`, {
      withCredentials: true
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateUsername(username: string): Observable<any> {
    return this.http.post(`${this.BASE_URL}/api/users/update-username`,
      { username: username },
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  verifyPassword(password: string): Observable<any> {
    return this.http.post(`${this.BASE_URL}/api/users/verify-password`,
      { password: password },
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  updatePassword(currentPassword: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.BASE_URL}/api/users/update-password`,
      { currentPassword: currentPassword, newPassword: newPassword },
      { headers: this.getHeaders() }
    ).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: any): Observable<never> {
    console.error('UserAccountService error:', error);
    return throwError(() => error);
  }
}