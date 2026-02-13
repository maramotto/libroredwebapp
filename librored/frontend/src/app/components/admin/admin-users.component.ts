import { Component, OnInit } from '@angular/core';
import { AdminService, UserDTO, PaginatedUsersResponse } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-users',
  templateUrl: './admin-users.component.html'
})
export class AdminUsersComponent implements OnInit {
  users: UserDTO[] = [];
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;
  loading: boolean = false;
  error: string = '';

  // Modal states
  showCreateModal: boolean = false;
  showEditModal: boolean = false;
  showDeleteModal: boolean = false;
  selectedUser: UserDTO | null = null;

  // Form data
  userForm: Omit<UserDTO, 'id'> = {
    username: '',
    email: '',
    password: '',
    role: 'ROLE_USER'
  };

  constructor(
    private adminService: AdminService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.error = '';

    this.adminService.getAllUsers(this.currentPage, this.pageSize).subscribe({
      next: (response: PaginatedUsersResponse) => {
        this.users = response.content;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalItems;
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Failed to load users. Please try again.';
        this.loading = false;
        console.error('Error loading users:', error);
      }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadUsers();
  }

  openCreateModal(): void {
    this.userForm = {
      username: '',
      email: '',
      role: 'ROLE_USER'
    };
    this.showCreateModal = true;
  }

  openEditModal(user: UserDTO): void {
    this.selectedUser = user;
    this.userForm = {
      username: user.username,
      email: user.email,
      role: user.role
    };
    this.showEditModal = true;
  }

  openDeleteModal(user: UserDTO): void {
    this.selectedUser = user;
    this.showDeleteModal = true;
  }

  closeModals(): void {
    this.showCreateModal = false;
    this.showEditModal = false;
    this.showDeleteModal = false;
    this.selectedUser = null;
  }

  createUser(): void {
    this.loading = true;
    this.error = '';

    this.adminService.createUser(this.userForm).subscribe({
      next: (user) => {
        this.loadUsers();
        this.closeModals();
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Failed to create user. Please try again.';
        this.loading = false;
        console.error('Error creating user:', error);
      }
    });
  }

  updateUser(): void {
    if (!this.selectedUser) return;

    this.loading = true;
    this.error = '';

    this.adminService.updateUser(this.selectedUser.id, this.userForm).subscribe({
      next: (user) => {
        this.loadUsers();
        this.closeModals();
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Failed to update user. Please try again.';
        this.loading = false;
        console.error('Error updating user:', error);
      }
    });
  }

  deleteUser(): void {
    if (!this.selectedUser) return;

    this.loading = true;
    this.error = '';

    this.adminService.deleteUser(this.selectedUser.id).subscribe({
      next: (response) => {
        if (response.selfDeletion) {
          // Admin deleted themselves - handle logout
          this.authService.logged = false;
          this.authService.user = undefined;
          // NO AUTH CHECK
        } else {
          this.loadUsers();
          this.closeModals();
        }
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Failed to delete user. Please try again.';
        this.loading = false;
        console.error('Error deleting user:', error);
      }
    });
  }

  downloadReport(): void {
    this.adminService.downloadAdminReport().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'Admin_Report.pdf';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
      },
      error: (error) => {
        this.error = 'Failed to download report. Please try again.';
        console.error('Error downloading report:', error);
      }
    });
  }

  get pageNumbers(): number[] {
    const pages = [];
    for (let i = 0; i < this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }
}