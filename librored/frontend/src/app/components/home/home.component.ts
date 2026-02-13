import { Component, OnInit } from '@angular/core';
import { BookService } from '../../services/book.service';
import { BookDTO } from '../../dtos/book.dto';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html'
})
export class HomeComponent implements OnInit {
  books: BookDTO[] = [];
  loading = false;
  loadingMore = false;
  errorMessage = '';

  // Pagination state
  currentPage = 0;
  pageSize = 8;
  hasMorePages = true;
  totalPages = 0;

  constructor(private bookService: BookService) {
  }

  ngOnInit(): void {
    this.loadBooks();
  }

  loadBooks(): void {
    this.loading = true;
    this.errorMessage = '';
    this.currentPage = 0;
    this.books = [];

    this.bookService.getBooksPaginated(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.books = response.content;
        this.currentPage = response.currentPage;
        this.totalPages = response.totalPages;
        this.hasMorePages = !response.last;
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = 'Failed to load books';
        this.loading = false;
      }
    });
  }

  loadMoreBooks(): void {
    if (!this.hasMorePages || this.loadingMore) {
      return;
    }

    this.loadingMore = true;
    this.errorMessage = '';
    const nextPage = this.currentPage + 1;

    this.bookService.getBooksPaginated(nextPage, this.pageSize).subscribe({
      next: (response) => {
        // Append new books to existing list
        this.books = [...this.books, ...response.content];
        this.currentPage = response.currentPage;
        this.totalPages = response.totalPages;
        this.hasMorePages = !response.last;
        this.loadingMore = false;
      },
      error: (error) => {
        this.errorMessage = 'Failed to load more books';
        this.loadingMore = false;
      }
    });
  }

  getImageUrl(book: BookDTO): string {
    return book.hasCoverImage ? this.bookService.getImageUrl(book.id!) : 'assets/default_cover.jpg';
  }
}