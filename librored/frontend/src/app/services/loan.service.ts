import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable, throwError } from "rxjs";
import { catchError } from "rxjs/operators";
import { LoanDTO, LoanRequest } from "../dtos/loan.dto";
import { PaginatedResponse } from "../interfaces/paginated-response.interface";

@Injectable({ providedIn: "root" })
export class LoanService {
  private readonly API_URL = "/api/loans"; // For public loans
  private readonly ADMIN_API_URL = "/api/v1/loans"; // For admin CRUD operations
  private readonly USER_LOANS_API_URL = "/api/v1/loans/lender"; // For user loan management

  constructor(private http: HttpClient) {}

  // Get all loans (for admin) or user's loans
  getLoans(): Observable<LoanDTO[]> {
    return this.http.get<LoanDTO[]>(this.API_URL, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  // Get loans by lender
  getLoansByLender(lenderId: number): Observable<LoanDTO[]> {
    return this.http.get<LoanDTO[]>(`${this.API_URL}/lender/${lenderId}`, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  // Get loans by borrower
  getLoansByBorrower(borrowerId: number): Observable<LoanDTO[]> {
    return this.http.get<LoanDTO[]>(`${this.API_URL}/borrower/${borrowerId}`, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  // Get single loan by ID
  getLoan(id: number): Observable<LoanDTO> {
    return this.http.get<LoanDTO>(`${this.ADMIN_API_URL}/${id}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * ADMIN METHODS - Full CRUD operations for admin users
   */

  /**
   * Get all loans with pagination (admin)
   */
  getAllLoansPaginated(page: number = 0, size: number = 10): Observable<PaginatedResponse<LoanDTO>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<LoanDTO>>(this.ADMIN_API_URL, { params })
      .pipe(catchError(this.handleError));
  }

  // Create new loan (admin)
  createLoan(loan: LoanRequest): Observable<LoanDTO> {
    // Convert LoanRequest to LoanDTO format expected by backend
    const loanDTO = {
      id: null, // null for creation
      book: loan.book,
      lender: loan.lender,
      borrower: loan.borrower,
      startDate: loan.startDate, // Backend expects LocalDate format (YYYY-MM-DD)
      endDate: loan.endDate || null, // null if not provided
      status: loan.status // Should match backend enum
    };

    return this.http.post<LoanDTO>(this.ADMIN_API_URL, loanDTO)
      .pipe(catchError(this.handleError));
  }

  // Update existing loan (admin)
  updateLoan(id: number, loan: LoanRequest): Observable<LoanDTO> {
    // Convert LoanRequest to LoanDTO format expected by backend
    const loanDTO = {
      id: id, // Include the ID for update
      book: loan.book,
      lender: loan.lender,
      borrower: loan.borrower,
      startDate: loan.startDate, // Backend expects LocalDate format (YYYY-MM-DD)
      endDate: loan.endDate || null, // null if not provided
      status: loan.status // Should match backend enum
    };

    console.log('=== LOAN SERVICE UPDATE DEBUG ===');
    console.log('Updating loan ID:', id);
    console.log('Input LoanRequest:', JSON.stringify(loan, null, 2));
    console.log('Converted loanDTO for backend:', JSON.stringify(loanDTO, null, 2));
    console.log('PUT URL:', `${this.ADMIN_API_URL}/${id}`);

    return this.http.put<LoanDTO>(`${this.ADMIN_API_URL}/${id}`, loanDTO)
      .pipe(catchError(this.handleError));
  }

  // Delete loan (admin)
  deleteLoan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.ADMIN_API_URL}/${id}`)
      .pipe(catchError(this.handleError));
  }

  // Get available books by lender ID (for loan creation/editing)
  getAvailableBooksByLender(lenderId: number): Observable<{id: number, title: string}[]> {
    return this.http.get<{id: number, title: string}[]>(`/api/v1/books/available/${lenderId}`)
      .pipe(catchError(this.handleError));
  }

  // Check if book is available for date range (for validation)
  isBookAvailableForDateRange(bookId: number, startDate: string, endDate?: string, excludeLoanId?: number): Observable<boolean> {
    let params: any = {
      bookId: bookId.toString(),
      startDate: startDate
    };
    
    if (endDate) params.endDate = endDate;
    if (excludeLoanId) params.excludeLoanId = excludeLoanId.toString();
    
    return this.http.get<boolean>(`${this.API_URL}/validate/book-availability`, { 
      params,
      withCredentials: true 
    }).pipe(catchError(this.handleError));
  }

  // Check if borrower is available for date range (for validation)
  isBorrowerAvailableForDateRange(borrowerId: number, lenderId: number, startDate: string, endDate?: string, excludeLoanId?: number): Observable<boolean> {
    let params: any = {
      borrowerId: borrowerId.toString(),
      lenderId: lenderId.toString(),
      startDate: startDate
    };
    
    if (endDate) params.endDate = endDate;
    if (excludeLoanId) params.excludeLoanId = excludeLoanId.toString();
    
    return this.http.get<boolean>(`${this.API_URL}/validate/borrower-availability`, {
      params,
      withCredentials: true
    }).pipe(catchError(this.handleError));
  }

  /**
   * USER LOAN MANAGEMENT METHODS - For managing loans where the user is the lender
   */

  // Get current user's loans (where they are the lender)
  getUserLoans(userId: number): Observable<LoanDTO[]> {
    return this.http.get<LoanDTO[]>(`${this.USER_LOANS_API_URL}/${userId}`, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  // Create new loan for current user (as lender)
  createUserLoan(loan: LoanRequest): Observable<LoanDTO> {
    const loanDTO = {
      id: null,
      book: loan.book,
      lender: loan.lender,
      borrower: loan.borrower,
      startDate: loan.startDate,
      endDate: loan.endDate || null,
      status: loan.status
    };

    return this.http.post<LoanDTO>(this.ADMIN_API_URL, loanDTO, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  // Update existing loan for current user (as lender)
  updateUserLoan(id: number, loan: LoanRequest): Observable<LoanDTO> {
    const loanDTO = {
      id: id,
      book: loan.book,
      lender: loan.lender,
      borrower: loan.borrower,
      startDate: loan.startDate,
      endDate: loan.endDate || null,
      status: loan.status
    };

    return this.http.put<LoanDTO>(`${this.ADMIN_API_URL}/${id}`, loanDTO, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  // Delete loan for current user (as lender)
  deleteUserLoan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.ADMIN_API_URL}/${id}`, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  // Get available books for current user (for loan creation)
  getUserAvailableBooks(userId: number): Observable<{id: number, title: string}[]> {
    return this.http.get<{id: number, title: string}[]>(`/api/v1/books/available/${userId}`, { withCredentials: true })
      .pipe(catchError(this.handleError));
  }

  private handleError(error: any): Observable<never> {
    console.error('LoanService error:', error);
    console.error('Full error object:', JSON.stringify(error, null, 2));
    let errorMessage = 'An error occurred';

    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    } else if (typeof error.error === 'string') {
      errorMessage = error.error;
    } else if (error.status) {
      errorMessage = `HTTP ${error.status}: ${error.statusText || 'Unknown error'}`;
    }

    return throwError(() => new Error(errorMessage));
  }
}