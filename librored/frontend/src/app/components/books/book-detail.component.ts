import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { BookService } from "../../services/book.service";
import { AuthService } from "../../services/auth.service";
import { BookDTO } from "../../dtos/book.dto";

@Component({
  selector: "book-detail",
  templateUrl: "./book-detail.component.html",
})
export class BookDetailComponent implements OnInit {
  book: BookDTO | null = null;
  loading = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bookService: BookService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Check authentication
    if (!this.authService.isLoggedIn()) {
      // NO AUTH CHECK
      return;
    }

    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadBook(id);
    } else {
      this.errorMessage = 'Invalid book ID';
    }
  }

  loadBook(id: number): void {
    this.loading = true;
    this.errorMessage = '';

    this.bookService.getBook(id).subscribe({
      next: (book) => {
        this.book = book;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading book:', error);
        this.errorMessage = 'Failed to load book details';
        this.loading = false;
      }
    });
  }

  editBook(): void {
    if (this.book) {
      this.router.navigate(['/books/edit', this.book.id]);
    }
  }

  deleteBook(): void {
    if (this.book && confirm(`Are you sure you want to delete "${this.book.title}"?`)) {
      this.bookService.deleteBook(this.book.id!).subscribe({
        next: () => {
          this.router.navigate(['/books']);
        },
        error: (error) => {
          console.error('Error deleting book:', error);
          this.errorMessage = 'Failed to delete book';
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/books']);
  }

  getImageUrl(): string {
    if (!this.book) return '/images/no_image.png';
    return this.book.hasCoverImage ? this.bookService.getImageUrl(this.book.id!) : '/images/no_image.png';
  }
}