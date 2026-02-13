import { Component, OnInit } from '@angular/core';
import { BookService } from '../../../services/book.service';
import { AdminService, UserDTO } from '../../../services/admin.service';
import { BookDTO } from '../../../dtos/book.dto';
import { PaginatedResponse } from '../../../interfaces/paginated-response.interface';

@Component({
  selector: 'app-admin-books',
  templateUrl: './admin-books.component.html',
  styleUrls: ['./admin-books.component.css']
})
export class AdminBooksComponent implements OnInit {
  books: BookDTO[] = [];
  users: UserDTO[] = [];

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalItems = 0;
  isLast = false;

  // Loading states
  loading = false;
  loadingUsers = false;

  // Modals
  showCreateModal = false;
  showEditModal = false;
  showDeleteModal = false;

  // Form data
  bookForm: BookDTO = this.getEmptyBookForm();
  selectedBookId: number | null = null;

  // Genres from backend enum
  genres = [
    'Fiction',
    'Non_Fiction',
    'Mystery_Thriller',
    'SciFi_Fantasy',
    'Romance',
    'Historical_Fiction',
    'Horror'
  ];

  // File upload
  selectedFile: File | null = null;

  // Error handling
  errorMessage = '';
  successMessage = '';

  constructor(
    private bookService: BookService,
    private adminService: AdminService
  ) {}

  ngOnInit(): void {
    this.loadBooks();
    this.loadUsers();
  }

  // === LOADING DATA ===

  loadBooks(): void {
    this.loading = true;
    this.errorMessage = '';

    this.bookService.getAllBooksPaginated(this.currentPage, this.pageSize).subscribe({
      next: (response: PaginatedResponse<BookDTO>) => {
        this.books = response.content;
        this.totalPages = response.totalPages || 0;
        this.totalItems = response.totalItems || 0;
        this.isLast = response.last || false;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading books:', error);
        this.errorMessage = 'Failed to load books';
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

  // === PAGINATION ===

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadBooks();
    }
  }

  nextPage(): void {
    if (!this.isLast) {
      this.currentPage++;
      this.loadBooks();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadBooks();
    }
  }

  // === CRUD OPERATIONS ===

  openCreateModal(): void {
    this.bookForm = this.getEmptyBookForm();
    this.selectedFile = null;
    this.showCreateModal = true;
    this.clearMessages();
  }

  openEditModal(book: BookDTO): void {
    this.bookForm = { ...book };
    this.selectedBookId = book.id!;
    this.selectedFile = null;
    this.showEditModal = true;
    this.clearMessages();
  }

  openDeleteModal(book: BookDTO): void {
    this.bookForm = book;
    this.selectedBookId = book.id!;
    this.showDeleteModal = true;
    this.clearMessages();
  }

  createBook(): void {
    if (!this.isFormValid()) {
      this.errorMessage = 'Please fill in all required fields';
      return;
    }

    this.loading = true;
    this.bookService.createBook(this.bookForm).subscribe({
      next: (createdBook) => {
        this.successMessage = 'Book created successfully!';
        this.closeCreateModal();
        this.loadBooks();

        // Upload cover image if selected
        if (this.selectedFile && createdBook.id) {
          this.uploadCoverImage(createdBook.id);
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error creating book:', error);
        this.errorMessage = 'Failed to create book';
        this.loading = false;
      }
    });
  }

  updateBook(): void {
    if (!this.isFormValid() || !this.selectedBookId) {
      this.errorMessage = 'Please fill in all required fields';
      return;
    }

    this.loading = true;
    this.bookService.updateBook(this.selectedBookId, this.bookForm).subscribe({
      next: () => {
        this.successMessage = 'Book updated successfully!';
        this.closeEditModal();
        this.loadBooks();

        // Upload cover image if selected
        if (this.selectedFile && this.selectedBookId) {
          this.uploadCoverImage(this.selectedBookId);
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error updating book:', error);
        this.errorMessage = 'Failed to update book';
        this.loading = false;
      }
    });
  }

  deleteBook(): void {
    if (!this.selectedBookId) return;

    this.loading = true;
    this.bookService.deleteBook(this.selectedBookId).subscribe({
      next: () => {
        this.successMessage = 'Book deleted successfully!';
        this.closeDeleteModal();
        this.loadBooks();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error deleting book:', error);
        this.errorMessage = 'Failed to delete book';
        this.loading = false;
      }
    });
  }

  // === FILE UPLOAD ===

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Please select an image file';
        return;
      }

      // Validate file size (5MB max)
      if (file.size > 5 * 1024 * 1024) {
        this.errorMessage = 'File size must be less than 5MB';
        return;
      }

      this.selectedFile = file;
      this.clearMessages();
    }
  }

  uploadCoverImage(bookId: number): void {
    if (!this.selectedFile) return;

    this.bookService.uploadCoverImage(bookId, this.selectedFile).subscribe({
      next: () => {
        console.log('Cover image uploaded successfully');
        this.loadBooks(); // Refresh to show updated cover
      },
      error: (error) => {
        console.error('Error uploading cover image:', error);
        this.errorMessage = 'Book saved but failed to upload cover image';
      }
    });
  }

  // === UTILITY METHODS ===

  getEmptyBookForm(): BookDTO {
    return {
      title: '',
      author: '',
      description: '',
      genre: '',
      hasCoverImage: false,
      owner: undefined
    };
  }

  isFormValid(): boolean {
    return !!(this.bookForm.title &&
              this.bookForm.author &&
              this.bookForm.description &&
              this.bookForm.genre &&
              this.bookForm.owner);
  }

  getCoverImageUrl(book: BookDTO): string {
    return book.hasCoverImage ?
      this.bookService.getCoverImageUrl(book.id!) :
      '/images/default_cover.jpg';
  }

  getUserName(userId: number): string {
    const user = this.users.find(u => u.id === userId);
    return user ? user.username : 'Unknown';
  }

  getPageNumbers(): number[] {
    const pages = [];
    for (let i = 0; i < this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  // === MODAL CONTROLS ===

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.bookForm = this.getEmptyBookForm();
    this.selectedFile = null;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.bookForm = this.getEmptyBookForm();
    this.selectedFile = null;
    this.selectedBookId = null;
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.bookForm = this.getEmptyBookForm();
    this.selectedBookId = null;
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
