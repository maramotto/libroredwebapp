import { Component, OnInit } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { BookService } from "../../services/book.service";
import { AuthService } from "../../services/auth.service";
import { BookDTO } from "../../dtos/book.dto";

@Component({
  selector: "book-form",
  templateUrl: "./book-form.component.html",
})
export class BookFormComponent implements OnInit {
  book: BookDTO = {
    title: '',
    description: '',
    hasCoverImage: false,
    shops: []
  };
  
  isEditMode = false;
  loading = false;
  submitting = false;
  errorMessage = '';
  successMessage = '';
  selectedImageFile: File | null = null;

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

    // Check if editing existing book
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.isEditMode = true;
      this.loadBook(id);
    }
  }

  loadBook(id: number): void {
    this.loading = true;
    this.bookService.getBook(id).subscribe({
      next: (book) => {
        this.book = book;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading book:', error);
        this.errorMessage = 'Failed to load book';
        this.loading = false;
      }
    });
  }

  onImageSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.selectedImageFile = file;
    }
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    
    if (!this.book.title.trim() || !this.book.description.trim()) {
      this.errorMessage = 'Title and description are required';
      return;
    }

    this.submitting = true;

    const operation = this.isEditMode 
      ? this.bookService.updateBook(this.book.id!, this.book)
      : this.bookService.createBook(this.book);

    operation.subscribe({
      next: (savedBook) => {
        this.successMessage = this.isEditMode ? 'Book updated successfully' : 'Book created successfully';
        
        // If there's an image to upload
        if (this.selectedImageFile) {
          this.uploadImage(savedBook.id!);
        } else {
          this.submitting = false;
          // Navigate back to book list after short delay
          setTimeout(() => {
            this.router.navigate(['/books']);
          }, 1500);
        }
      },
      error: (error) => {
        console.error('Error saving book:', error);
        this.errorMessage = 'Failed to save book';
        this.submitting = false;
      }
    });
  }

  uploadImage(bookId: number): void {
    if (this.selectedImageFile) {
      this.bookService.uploadCoverImage(bookId, this.selectedImageFile).subscribe({
        next: () => {
          this.successMessage = 'Book and image saved successfully';
          this.submitting = false;
          // Navigate back to book list after short delay
          setTimeout(() => {
            this.router.navigate(['/books']);
          }, 1500);
        },
        error: (error) => {
          console.error('Error uploading image:', error);
          this.errorMessage = 'Book saved but image upload failed';
          this.submitting = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/books']);
  }
}