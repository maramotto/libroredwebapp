import { Component, OnInit } from '@angular/core';
import { LoanService } from '../../../services/loan.service';
import { AdminService, UserDTO } from '../../../services/admin.service';
import { BookService } from '../../../services/book.service';
import { LoanDTO, LoanRequest, LoanStatus, BookBasicDTO, UserBasicDTO } from '../../../dtos/loan.dto';
import { PaginatedResponse } from '../../../interfaces/paginated-response.interface';

@Component({
  selector: 'app-admin-loans',
  templateUrl: './admin-loans.component.html',
  styleUrls: ['./admin-loans.component.css']
})
export class AdminLoansComponent implements OnInit {
  loans: LoanDTO[] = [];
  users: UserDTO[] = [];
  availableBooks: any[] = [];

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalItems = 0;
  isLast = false;

  // Loading states
  loading = false;
  loadingUsers = false;
  loadingBooks = false;

  // Modals
  showCreateModal = false;
  showEditModal = false;
  showDeleteModal = false;

  // Form data (using IDs for form binding, converting to nested structure on submit)
  loanForm: {
    bookId: any;
    lenderId: any;
    borrowerId: any;
    startDate: string;
    endDate: string;
    status: LoanStatus;
  } = this.getEmptyLoanForm();
  selectedLoanId: number | null = null;

  // Loan statuses from enum
  loanStatuses = [
    { value: LoanStatus.Active, label: 'Active' },
    { value: LoanStatus.Completed, label: 'Completed' }
  ];

  // Error handling
  errorMessage = '';
  successMessage = '';

  constructor(
    private loanService: LoanService,
    private adminService: AdminService,
    private bookService: BookService
  ) {}

  ngOnInit(): void {
    this.loadLoans();
    this.loadUsers();
  }

  // === LOADING DATA ===

  loadLoans(): void {
    this.loading = true;
    this.errorMessage = '';

    this.loanService.getAllLoansPaginated(this.currentPage, this.pageSize).subscribe({
      next: (response: PaginatedResponse<LoanDTO>) => {
        this.loans = response.content;
        this.totalPages = response.totalPages || 0;
        this.totalItems = response.totalItems || 0;
        this.isLast = response.last || false;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading loans:', error);
        this.errorMessage = 'Failed to load loans';
        this.loading = false;
      }
    });
  }

  loadUsers(): void {
    this.loadingUsers = true;
    this.adminService.getAllUsers(0, 100).subscribe({
      next: (response) => {
        this.users = response.content;
        this.loadingUsers = false;
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.loadingUsers = false;
      }
    });
  }

  loadAvailableBooks(lenderId: number): void {
    if (!lenderId) {
      this.availableBooks = [];
      return;
    }

    console.log('Loading books for lender ID:', lenderId);
    this.loadingBooks = true;
    this.loanService.getAvailableBooksByLender(lenderId).subscribe({
      next: (books) => {
        console.log('Received books:', books);
        this.availableBooks = books;
        this.loadingBooks = false;
      },
      error: (error) => {
        console.error('Error loading books:', error);
        console.error('Error details:', error.status, error.statusText, error.error);
        this.availableBooks = [];
        this.loadingBooks = false;
      }
    });
  }

  loadAvailableBooksForEdit(lenderId: number, currentBook: any): void {
    if (!lenderId) {
      this.availableBooks = [];
      return;
    }

    console.log('Loading books for edit - lender ID:', lenderId, 'current book:', currentBook);
    this.loadingBooks = true;
    this.loanService.getAvailableBooksByLender(lenderId).subscribe({
      next: (books) => {
        console.log('Received available books:', books);
        // Add the current book to the list if it's not already there
        const currentBookInList = books.find(book => book.id === currentBook.id);
        if (!currentBookInList && currentBook.id) {
          books.unshift({
            id: currentBook.id,
            title: currentBook.title
          });
          console.log('Added current book to list:', currentBook.title);
        }
        this.availableBooks = books;
        this.loadingBooks = false;
      },
      error: (error) => {
        console.error('Error loading books for edit:', error);
        // If loading fails, at least show the current book
        this.availableBooks = currentBook.id ? [{
          id: currentBook.id,
          title: currentBook.title
        }] : [];
        this.loadingBooks = false;
      }
    });
  }

  // === PAGINATION ===

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadLoans();
    }
  }

  nextPage(): void {
    if (!this.isLast) {
      this.currentPage++;
      this.loadLoans();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadLoans();
    }
  }

  // === CRUD OPERATIONS ===

  openCreateModal(): void {
    this.loanForm = this.getEmptyLoanForm();
    this.availableBooks = [];
    this.showCreateModal = true;
    this.clearMessages();
  }

  openEditModal(loan: LoanDTO): void {
    this.loanForm = {
      bookId: loan.book.id,
      lenderId: loan.lender.id,
      borrowerId: loan.borrower.id,
      startDate: loan.startDate,
      endDate: loan.endDate || '',
      status: loan.status
    };
    this.selectedLoanId = loan.id!;
    this.loadAvailableBooksForEdit(loan.lender.id, loan.book);
    this.showEditModal = true;
    this.clearMessages();
  }

  openDeleteModal(loan: LoanDTO): void {
    this.selectedLoanId = loan.id!;
    this.showDeleteModal = true;
    this.clearMessages();
  }

  createLoan(): void {
    if (!this.isFormValid()) {
      this.errorMessage = 'Please fill in all required fields';
      return;
    }

    this.loading = true;
    const loanRequest = this.convertFormToLoanRequest();
    console.log('Creating loan with data:', JSON.stringify(loanRequest, null, 2));
    this.loanService.createLoan(loanRequest).subscribe({
      next: () => {
        this.successMessage = 'Loan created successfully!';
        this.closeCreateModal();
        this.loadLoans();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error creating loan:', error);
        this.errorMessage = 'Failed to create loan: ' + error.message;
        this.loading = false;
      }
    });
  }

  updateLoan(): void {
    if (!this.isFormValid() || !this.selectedLoanId) {
      this.errorMessage = 'Please fill in all required fields';
      return;
    }

    this.loading = true;
    const loanRequest = this.convertFormToLoanRequest();
    console.log('=== UPDATE LOAN DEBUG ===');
    console.log('Selected loan ID:', this.selectedLoanId);
    console.log('Form data before conversion:', this.loanForm);
    console.log('Available books:', this.availableBooks);
    console.log('Users list:', this.users);
    console.log('Converted loan request:', JSON.stringify(loanRequest, null, 2));

    this.loanService.updateLoan(this.selectedLoanId, loanRequest).subscribe({
      next: () => {
        console.log('✅ Loan updated successfully');
        this.successMessage = 'Loan updated successfully!';
        this.closeEditModal();
        this.loadLoans();
        this.loading = false;
      },
      error: (error) => {
        console.error('❌ Error updating loan:', error);
        console.error('Error status:', error.status);
        console.error('Error message:', error.message);
        console.error('Full error object:', JSON.stringify(error, null, 2));
        this.errorMessage = 'Failed to update loan: ' + error.message;
        this.loading = false;
      }
    });
  }

  deleteLoan(): void {
    if (!this.selectedLoanId) return;

    this.loading = true;
    this.loanService.deleteLoan(this.selectedLoanId).subscribe({
      next: () => {
        this.successMessage = 'Loan deleted successfully!';
        this.closeDeleteModal();
        this.loadLoans();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error deleting loan:', error);
        this.errorMessage = 'Failed to delete loan: ' + error.message;
        this.loading = false;
      }
    });
  }

  // === EVENT HANDLERS ===

  onLenderChange(): void {
    this.loanForm.bookId = 0; // Reset book selection
    this.loadAvailableBooks(Number(this.loanForm.lenderId));
  }

  // === UTILITY METHODS ===

  getEmptyLoanForm() {
    return {
      bookId: 0,
      lenderId: 0,
      borrowerId: 0,
      startDate: '',
      endDate: '',
      status: LoanStatus.Active
    };
  }

  convertFormToLoanRequest(): LoanRequest {
    console.log('Form data:', this.loanForm);
    console.log('Form book ID type:', typeof this.loanForm.bookId, 'value:', this.loanForm.bookId);
    console.log('Form lender ID type:', typeof this.loanForm.lenderId, 'value:', this.loanForm.lenderId);
    console.log('Form borrower ID type:', typeof this.loanForm.borrowerId, 'value:', this.loanForm.borrowerId);
    console.log('Available books:', this.availableBooks);
    console.log('Users:', this.users);

    const bookId = Number(this.loanForm.bookId);
    const lenderId = Number(this.loanForm.lenderId);
    const borrowerId = Number(this.loanForm.borrowerId);

    console.log('Converted IDs:', { bookId, lenderId, borrowerId });

    const selectedBook = this.availableBooks.find(book => book.id === bookId);
    const selectedLender = this.users.find(user => user.id === lenderId);
    const selectedBorrower = this.users.find(user => user.id === borrowerId);

    console.log('Selected book:', selectedBook);
    console.log('Selected lender:', selectedLender);
    console.log('Selected borrower:', selectedBorrower);

    return {
      book: {
        id: selectedBook?.id || 0,
        title: selectedBook?.title || '',
        author: selectedBook?.author || ''
      },
      lender: {
        id: selectedLender?.id || 0,
        username: selectedLender?.username || ''
      },
      borrower: {
        id: selectedBorrower?.id || 0,
        username: selectedBorrower?.username || ''
      },
      startDate: this.loanForm.startDate,
      endDate: this.loanForm.endDate || undefined,
      status: this.loanForm.status
    };
  }

  isFormValid(): boolean {
    return !!(this.loanForm.bookId && this.loanForm.bookId !== 0 &&
              this.loanForm.lenderId && this.loanForm.lenderId !== 0 &&
              this.loanForm.borrowerId && this.loanForm.borrowerId !== 0 &&
              this.loanForm.startDate &&
              this.loanForm.status &&
              this.loanForm.lenderId !== this.loanForm.borrowerId);
  }


  getStatusBadgeClass(status: LoanStatus): string {
    return status === LoanStatus.Active ? 'bg-success' : 'bg-secondary';
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'No end date';
    return new Date(dateString).toLocaleDateString();
  }

  getPageNumbers(): number[] {
    const pages = [];
    for (let i = 0; i < this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  getEndIndex(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalItems);
  }

  getSelectedLoan(): LoanDTO | undefined {
    return this.loans.find(loan => loan.id === this.selectedLoanId);
  }

  // === MODAL CONTROLS ===

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.loanForm = this.getEmptyLoanForm();
    this.availableBooks = [];
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.loanForm = this.getEmptyLoanForm();
    this.availableBooks = [];
    this.selectedLoanId = null;
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.selectedLoanId = null;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
