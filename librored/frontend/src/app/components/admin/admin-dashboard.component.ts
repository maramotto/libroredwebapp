import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html'
})
export class AdminDashboardComponent implements OnInit {

  constructor(
    private router: Router,
    private adminService: AdminService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // NO AUTH CHECK - Let backend handle it
    console.log('AdminDashboard loaded without auth check!');
  }

  navigateToUserManagement(): void {
    this.router.navigate(['/admin/users']);
  }

  navigateToBookManagement(): void {
    this.router.navigate(['/admin/books']);
  }

  navigateToLoanManagement(): void {
    this.router.navigate(['/admin/loans']);
  }

  downloadReport(): void {
    console.log('Download report button clicked');
    this.adminService.downloadAdminReport().subscribe({
      next: (blob) => {
        console.log('Received blob:', blob);
        console.log('Blob size:', blob.size);
        console.log('Blob type:', blob.type);

        // Check if blob is valid
        if (blob.size === 0) {
          console.error('Received empty blob');
          alert('Received empty file. Please try again.');
          return;
        }

        // Check if blob is actually a PDF
        if (blob.type !== 'application/pdf' && blob.type !== '') {
          console.error('Unexpected blob type:', blob.type);
          // Still try to download, but warn
        }

        try {
          // Create blob URL
          const url = window.URL.createObjectURL(blob);

          // Create temporary link and trigger download
          const link = document.createElement('a');
          link.href = url;
          link.download = 'Admin_Report.pdf';
          link.target = '_blank';

          // Ensure link is invisible and add to DOM
          link.style.display = 'none';
          link.style.visibility = 'hidden';
          document.body.appendChild(link);

          // Trigger the download
          link.click();

          // Clean up immediately
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);

          console.log('Download initiated successfully');
        } catch (e) {
          console.error('Error creating download:', e);
          alert('Error creating download. Please try again.');
        }
      },
      error: (error) => {
        console.error('Error downloading report:', error);
        console.error('Error status:', error.status);
        console.error('Error message:', error.message);

        if (error.status === 401 || error.status === 403) {
          alert('You do not have permission to download the admin report. Please ensure you are logged in as an administrator.');
        } else if (error.status === 0) {
          alert('Network error. Please check if the backend is running and try again.');
        } else {
          alert(`Failed to download report. Status: ${error.status}. Please try again.`);
        }
      }
    });
  }

  logout(): void {
    this.authService.logOut();
  }
}