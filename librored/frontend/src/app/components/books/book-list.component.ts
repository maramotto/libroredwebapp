import { Component, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { BookService } from "../../services/book.service";
import { AuthService } from "../../services/auth.service";
import { BookDTO } from "../../dtos/book.dto";

@Component({
  selector: "book-list",
  templateUrl: "./book-list.component.html",
})
export class BookListComponent implements OnInit {
  books: BookDTO[] = [];
  loading = false;
  errorMessage = '';

  constructor(
    private bookService: BookService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Load books for all users (like MVC index page)
    this.loadBooks();
  }

  loadBooks(): void {
    this.loading = true;
    this.errorMessage = '';
    
    this.bookService.getBooks().subscribe({
      next: (books) => {
        this.books = books;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading books:', error);
        this.errorMessage = 'Failed to load books';
        this.loading = false;
      }
    });
  }

  editBook(book: BookDTO): void {
    this.router.navigate(['/books/edit', book.id]);
  }

  deleteBook(book: BookDTO): void {
    if (confirm(`Are you sure you want to delete "${book.title}"?`)) {
      this.bookService.deleteBook(book.id!).subscribe({
        next: () => {
          this.loadBooks(); // Reload list after delete
        },
        error: (error) => {
          console.error('Error deleting book:', error);
          this.errorMessage = 'Failed to delete book';
        }
      });
    }
  }

  getImageUrl(book: BookDTO): string {
    return book.hasCoverImage ? this.bookService.getImageUrl(book.id!) : '/images/no_image.png';
  }
}