package es.codeurjc13.librored.controller;

import es.codeurjc13.librored.model.Book;
import es.codeurjc13.librored.model.User;
import es.codeurjc13.librored.service.BookService;
import es.codeurjc13.librored.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;


@Controller
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @ModelAttribute
    public void addAttributes(Model model, HttpServletRequest request) {

        Principal principal = request.getUserPrincipal();

        if (principal != null) {

            model.addAttribute("logged", true);
            model.addAttribute("userName", principal.getName());
            model.addAttribute("admin", request.isUserInRole("ADMIN"));

        } else {
            model.addAttribute("logged", false);
        }
    }


    @GetMapping("/")
    public String index(Model model) {

        // Fetch books from the database
        List<Book> books = bookService.findAll();
        model.addAttribute("books", books); // Ensure books list is passed to the view
        return "index";
    }

    // Render the books.mustache template with all books
    @GetMapping("/books")
    public String listBooks(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRole() == User.Role.ROLE_ADMIN;

        List<Book> books = isAdmin ? bookService.getAllBooks() : bookService.getBooksByOwner(user);

        model.addAttribute("books", books);
        model.addAttribute("isAdmin", isAdmin);
        return "books";
    }


    // CREATE
    @GetMapping("/books/create")
    public String createBookForm(@AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRole() == User.Role.ROLE_ADMIN; // Correct way to check admin role

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("userId", user.getId()); // Pass logged-in user's ID for non-admins
        model.addAttribute("book", new Book()); // Ensure book attribute is always available
        model.addAttribute("genreList", Book.Genre.values()); // Pass list of genres

        if (isAdmin) {
            model.addAttribute("users", userService.getAllUsers()); // Load users for admin dropdown
        }

        return "create-book";
    }


    @PostMapping("/books/create")
    public String createBook(@RequestParam("title") String title, @RequestParam("author") String author, @RequestParam("genre") String genre, @RequestParam("description") String description, @RequestParam("ownerId") Long ownerId, @RequestParam(value = "coverImage", required = false) MultipartFile coverImage, RedirectAttributes redirectAttributes) {

        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(Book.Genre.valueOf(genre));
        book.setDescription(description);
        book.setOwner(userService.getUserById(ownerId).orElseThrow(() -> new RuntimeException("Owner not found")));

        try {
            if (coverImage != null && !coverImage.isEmpty()) {
                // Convert uploaded image to Blob and store it
                book.setCoverPic(new SerialBlob(coverImage.getBytes()));
            } else {
                // Use the default cover image from resources
                byte[] defaultImageBytes = Files.readAllBytes(Paths.get("src/main/resources/static/images/default_cover.jpg"));
                book.setCoverPic(new SerialBlob(defaultImageBytes));
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Error processing cover image", e);
        }

        bookService.saveBook(book);
        redirectAttributes.addFlashAttribute("message", "Book created successfully!");

        return "redirect:/books";
    }


    // PAINT IMAGE FROM DB
    @GetMapping("/books/{id}/image")
    public ResponseEntity<Object> downloadImage(@PathVariable long id) throws SQLException {

        Optional<Book> book = bookService.getBookById(id);
        if (book.isPresent() && book.get().getCoverPic() != null) {

            Resource file = new InputStreamResource(book.get().getCoverPic().getBinaryStream());

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, "image/jpeg").contentLength(book.get().getCoverPic().length()).body(file);

        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private void setCoverPic(Book book, MultipartFile imageField) throws IOException {

        if (!imageField.isEmpty()) {
            book.setCoverPic(BlobProxy.generateProxy(imageField.getInputStream(), imageField.getSize()));
        }
    }


    // EDIT
    @GetMapping("/books/edit/{id}")
    public String editBookForm(@PathVariable Long id, @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails, Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

        Book book = bookService.getBookById(id).orElseThrow(() -> new RuntimeException("Book not found"));

        boolean isAdmin = user.getRole() == User.Role.ROLE_ADMIN; // Correct way to check admin role


        // Allow users to edit only their own books
        if (!isAdmin && !book.getOwner().getId().equals(user.getId())) {
            return "redirect:/books";  // Redirect to `/books` if they try to edit someone else's book
        }


        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("book", book);
        model.addAttribute("genreList", Book.Genre.values());

        if (isAdmin) {
            model.addAttribute("users", userService.getAllUsers()); // Load users only for admins
        }

        return "edit-book";
    }

    @PostMapping("/books/edit/{id}")
    public String updateBook(@PathVariable Long id, @RequestParam String title, @RequestParam String author, @RequestParam Book.Genre genre, @RequestParam String description, @RequestParam(required = false) MultipartFile coverPic, 
                             @RequestParam Long ownerId) throws IOException {

        Book book = bookService.getBookById(id).orElseThrow(() -> new IllegalArgumentException("Invalid book ID"));

        User owner = userService.getUserById(ownerId).orElseThrow(() -> new IllegalArgumentException("Invalid user ID"));

        book.setTitle(title);
        book.setAuthor(author);
        book.setGenre(genre);
        book.setDescription(description);
        book.setOwner(owner);

        if (coverPic != null && !coverPic.isEmpty()) {
            setCoverPic(book, coverPic);
        }

        bookService.saveBook(book);
        return "redirect:/books";
    }

    @PostMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return "redirect:/books";
    }


    // RECOMMENDED BOOKS
    @GetMapping("/recommendations")
    public String getAccountPage(Model model, @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userService.getUserByEmail(userDetails.getUsername()).orElseThrow(() -> new RuntimeException("User not found"));

        List<Book> recommendedBooks = bookService.getRecommendationsForUser(user.getId());
        model.addAttribute("recommendedBooks", recommendedBooks);
        model.addAttribute("user", user);

        return "recommendations";
    }

}
