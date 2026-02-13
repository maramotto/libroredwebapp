import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { BookDTO } from '../dtos/book.dto';
import { PaginatedResponse } from '../interfaces/paginated-response.interface';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class BookService {
  private readonly API_URL = '/api/books'; // For public books
  private readonly ADMIN_API_URL = '/api/v1/books'; // For admin CRUD operations
  private readonly BASE_URL = 'https://localhost:8443';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  private getHeaders(): HttpHeaders {
    const token = this.authService.getAccessToken();
    console.log('🔐 BookService - JWT token for request:', token ? 'Token present' : 'No token found');
    console.log('🔐 BookService - Full token value:', token);
    if (!token) {
      console.warn('⚠️ No JWT token available - request will fail authentication');
    }
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
    console.log('🔐 BookService - Authorization header:', headers.get('Authorization'));
    return headers;
  }

  /**
   * Get all books (legacy method for backwards compatibility)
   */
  getBooks(): Observable<BookDTO[]> {
    return this.getBooksPaginated(0, 8).pipe(
      map(response => response.content)
    );
  }

  /**
   * Get books with pagination
   */
  getBooksPaginated(page: number = 0, size: number = 8): Observable<PaginatedResponse<BookDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<BookDTO>>(this.API_URL, {
      params,
      withCredentials: true
    });
  }

  /**
   * Get book by ID
   */
  getBook(id: number): Observable<BookDTO> {
    return this.http.get<BookDTO>(`${this.ADMIN_API_URL}/${id}`);
  }

  /**
   * Get books by owner ID
   */
  getBooksByOwner(ownerId: number): Observable<BookDTO[]> {
    return this.http.get<BookDTO[]>(`${this.BASE_URL}${this.ADMIN_API_URL}/owner/${ownerId}`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * ADMIN METHODS - Full CRUD operations for admin users
   */

  /**
   * Get all books with pagination (admin)
   */
  getAllBooksPaginated(page: number = 0, size: number = 10): Observable<PaginatedResponse<BookDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<BookDTO>>(this.ADMIN_API_URL, { params });
  }

  /**
   * Create new book (admin)
   */
  createBook(book: BookDTO): Observable<BookDTO> {
    return this.http.post<BookDTO>(`${this.BASE_URL}${this.ADMIN_API_URL}`, book, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Update existing book (admin)
   */
  updateBook(id: number, book: BookDTO): Observable<BookDTO> {
    return this.http.put<BookDTO>(`${this.BASE_URL}${this.ADMIN_API_URL}/${id}`, book, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Delete book (admin)
   */
  deleteBook(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE_URL}${this.ADMIN_API_URL}/${id}`, {
      headers: this.getHeaders()
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Upload book cover image (admin)
   */
  uploadCoverImage(id: number, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    console.log('📤 Uploading cover image for book ID:', id, 'File:', file.name);

    // Get JWT token for authorization
    const token = this.authService.getAccessToken();
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.post(`${this.BASE_URL}${this.ADMIN_API_URL}/${id}/cover`, formData, {
      headers: headers
    }).pipe(
      catchError(this.handleError)
    );
  }

  /**
   * Get book cover image URL
   */
  getCoverImageUrl(id: number): string {
    return `${this.BASE_URL}${this.API_URL}/${id}/cover`;
  }

  /**
   * Get book image URL (legacy)
   */
  getImageUrl(id: number): string {
    return `${this.API_URL}/${id}/cover`;
  }

  private handleError(error: any): Observable<never> {
    console.error('BookService error:', error);
    return throwError(() => error);
  }
}