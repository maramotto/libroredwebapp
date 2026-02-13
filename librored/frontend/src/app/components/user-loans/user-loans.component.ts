import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LoanService } from '../../services/loan.service';
import { AuthService } from '../../services/auth.service';
import { UserService } from '../../services/user.service';
import { UserAccountService } from '../../services/user-account.service';
import { AdminService, UserDTO } from '../../services/admin.service';
import { LoanDTO, LoanStatus, LoanRequest } from '../../dtos/loan.dto';

@Component({
  selector: 'app-user-loans',
  templateUrl: './user-loans.component.html',
  styleUrls: ['./user-loans.component.css']
})
export class UserLoansComponent implements OnInit {
  loans: LoanDTO[] = [];
  currentUser: any = null;
  users: UserDTO[] = [];
  availableBooks: any[] = [];

  // Loading states
  loading = false;
  loadingUsers = false;
  loadingBooks = false;

  // Modal states
  showCreateModal = false;
  showEditModal = false;
  showDeleteModal = false;

  // Form data
  loanForm: {
    bookId: any;
    lenderId: any;
    borrowerId: any;
    startDate: string;
    endDate: string;
    status: LoanStatus;
  } = this.getEmptyLoanForm();
  selectedLoanId: number | null = null;

  // Loan statuses
  loanStatuses = [
    { value: LoanStatus.Active, label: 'Active' },
    { value: LoanStatus.Completed, label: 'Completed' }
  ];

  // Messages
  errorMessage = '';
  successMessage = '';

  constructor(
    private loanService: LoanService,
    private authService: AuthService,
    private userService: UserService,
    private userAccountService: UserAccountService,
    private adminService: AdminService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCurrentUser();
  }

  loadCurrentUser(): void {
    this.loading = true;
    this.userAccountService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser = user;
        this.loadUserLoans();
        this.loadUsers();
      },
      error: (error) => {
        console.error('Error loading current user:', error);
        this.errorMessage = 'Failed to load user information';
        this.loading = false;
        if (error.status === 401) {
          this.router.navigate(['/login']);
        }
      }
    });
  }

  loadUserLoans(): void {
    if (!this.currentUser?.id) return;

    this.loanService.getUserLoans(this.currentUser.id).subscribe({
      next: (loans) => {
        this.loans = loans;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading user loans:', error);
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

    this.loadingBooks = true;
    this.loanService.getAvailableBooksByLender(lenderId).subscribe({
      next: (books) => {
        this.availableBooks = books;
        this.loadingBooks = false;
      },
      error: (error) => {
        console.error('Error loading books:', error);
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

    this.loadingBooks = true;
    this.loanService.getAvailableBooksByLender(lenderId).subscribe({
      next: (books) => {
        // Add current loan's book if it's not already in the list
        const currentBookExists = books.some(book => book.id === currentBook.id);
        if (!currentBookExists) {
          books.unshift({
            id: currentBook.id,
            title: currentBook.title
          });
        }
        this.availableBooks = books;
        this.loadingBooks = false;
      },
      error: (error) => {
        console.error('Error loading books:', error);
        // If error, at least include the current book
        this.availableBooks = [{
          id: currentBook.id,
          title: currentBook.title
        }];
        this.loadingBooks = false;
      }
    });
  }

  // Modal methods
  openCreateModal(): void {
    this.loanForm = this.getEmptyLoanForm();
    // Auto-set current user as lender
    this.loanForm.lenderId = this.currentUser.id;
    this.availableBooks = [];
    this.showCreateModal = true;
    this.clearMessages();
    // Load books for current user
    this.loadAvailableBooks(this.currentUser.id);
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
    this.selectedLoanId = loan.id;
    this.showEditModal = true;
    this.clearMessages();
    // Load books for the lender and include current loan's book
    this.loadAvailableBooksForEdit(loan.lender.id, loan.book);
  }

  openDeleteModal(loan: LoanDTO): void {
    this.selectedLoanId = loan.id;
    this.showDeleteModal = true;
    this.clearMessages();
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.loanForm = this.getEmptyLoanForm();
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.loanForm = this.getEmptyLoanForm();
    this.selectedLoanId = null;
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.selectedLoanId = null;
  }

  onLenderChange(): void {
    this.loanForm.bookId = 0; // Reset book selection
    this.loadAvailableBooks(Number(this.loanForm.lenderId));
  }

  createLoan(): void {
    if (!this.validateForm()) return;

    this.loading = true;
    const loanRequest = this.convertFormToLoanRequest();

    this.loanService.createUserLoan(loanRequest).subscribe({
      next: () => {
        this.successMessage = 'Loan created successfully';
        this.closeCreateModal();
        this.loadUserLoans();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error creating loan:', error);
        this.errorMessage = error.message || 'Failed to create loan';
        this.loading = false;
      }
    });
  }

  updateLoan(): void {
    if (!this.validateForm() || !this.selectedLoanId) return;

    this.loading = true;
    const loanRequest = this.convertFormToLoanRequest();

    this.loanService.updateUserLoan(this.selectedLoanId, loanRequest).subscribe({
      next: () => {
        this.successMessage = 'Loan updated successfully';
        this.closeEditModal();
        this.loadUserLoans();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error updating loan:', error);
        this.errorMessage = error.message || 'Failed to update loan';
        this.loading = false;
      }
    });
  }

  deleteLoan(): void {
    if (!this.selectedLoanId) return;

    this.loading = true;
    this.loanService.deleteUserLoan(this.selectedLoanId).subscribe({
      next: () => {
        this.successMessage = 'Loan deleted successfully';
        this.closeDeleteModal();
        this.loadUserLoans();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error deleting loan:', error);
        this.errorMessage = 'Failed to delete loan';
        this.loading = false;
      }
    });
  }

  validateForm(): boolean {
    if (!this.loanForm.bookId) {
      this.errorMessage = 'Please select a book';
      return false;
    }
    if (!this.loanForm.borrowerId) {
      this.errorMessage = 'Please select a borrower';
      return false;
    }
    if (!this.loanForm.startDate) {
      this.errorMessage = 'Start date is required';
      return false;
    }
    return true;
  }

  convertFormToLoanRequest(): LoanRequest {
    const selectedBook = this.availableBooks.find(book => book.id == this.loanForm.bookId);
    const selectedLender = this.users.find(user => user.id == this.loanForm.lenderId);
    const selectedBorrower = this.users.find(user => user.id == this.loanForm.borrowerId);

    return {
      book: {
        id: selectedBook?.id || 0,
        title: selectedBook?.title || '',
        author: ''
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

  getEmptyLoanForm() {
    const today = new Date();
    return {
      bookId: 0,
      lenderId: this.currentUser?.id || 0,
      borrowerId: 0,
      startDate: today.toISOString().split('T')[0],
      endDate: '',
      status: LoanStatus.Active
    };
  }

  getSelectedLoan(): LoanDTO | undefined {
    return this.loans.find(loan => loan.id === this.selectedLoanId);
  }

  getStatusBadgeClass(status: LoanStatus): string {
    switch (status) {
      case LoanStatus.Active:
        return 'badge bg-success';
      case LoanStatus.Completed:
        return 'badge bg-secondary';
      default:
        return 'badge bg-secondary';
    }
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return 'Not specified';
    return new Date(dateStr).toLocaleDateString();
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
