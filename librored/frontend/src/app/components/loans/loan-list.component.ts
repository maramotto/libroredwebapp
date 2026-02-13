import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { LoanService } from "../../services/loan.service";
import { AuthService } from "../../services/auth.service";
import { LoanDTO } from "../../dtos/loan.dto";

@Component({
  selector: "loan-list",
  templateUrl: "./loan-list.component.html",
})
export class LoanListComponent implements OnInit {
  loans: LoanDTO[] = [];
  loading = false;
  errorMessage = '';
  isAdmin = false;
  currentUserId: number | null = null;

  constructor(
    private loanService: LoanService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // NO AUTH CHECK - Let backend handle it
    this.loadLoans();
  }

  loadLoans(): void {
    this.loading = true;
    this.errorMessage = '';
    
    this.loanService.getLoans().subscribe({
      next: (loans) => {
        this.loans = loans;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading loans:', error);
        this.errorMessage = 'Failed to load loans';
        this.loading = false;
      }
    });
  }

  editLoan(loan: LoanDTO): void {
    this.router.navigate(['/loans/edit', loan.id]);
  }

  deleteLoan(loan: LoanDTO): void {
    if (confirm(`Are you sure you want to delete this loan for "${loan.book?.title || 'Unknown Book'}"?`)) {
      this.loanService.deleteLoan(loan.id!).subscribe({
        next: () => {
          this.loadLoans(); // Reload list after delete
        },
        error: (error) => {
          console.error('Error deleting loan:', error);
          this.errorMessage = 'Failed to delete loan';
        }
      });
    }
  }

  canEditLoan(loan: LoanDTO): boolean {
    // Admin can edit any loan, user can edit loans where they are the lender
    return this.isAdmin || loan.lender?.id === this.currentUserId;
  }

  canDeleteLoan(loan: LoanDTO): boolean {
    // Admin can delete any loan, user can delete loans where they are the lender
    return this.isAdmin || loan.lender?.id === this.currentUserId;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Active':
        return 'badge bg-success';
      case 'Completed':
        return 'badge bg-secondary';
      default:
        return 'badge bg-light';
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString();
  }

  isOverdue(loan: LoanDTO): boolean {
    if (!loan.endDate || loan.status === 'Completed') return false;
    const today = new Date();
    const endDate = new Date(loan.endDate);
    return endDate < today;
  }

  createNewLoan(): void {
    this.router.navigate(['/loans/create']);
  }
}