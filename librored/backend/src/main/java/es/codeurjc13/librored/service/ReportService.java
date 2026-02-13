package es.codeurjc13.librored.service;

import es.codeurjc13.librored.model.Book;
import es.codeurjc13.librored.model.Loan;
import es.codeurjc13.librored.model.User;
import es.codeurjc13.librored.util.PDFPageHelper;
import es.codeurjc13.librored.util.PDFResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


@Service
public class ReportService {

    private final UserService userService;
    private final BookService bookService;
    private final LoanService loanService;

    public ReportService(UserService userService, BookService bookService, LoanService loanService) {
        this.userService = userService;
        this.bookService = bookService;
        this.loanService = loanService;
    }

    // Generate a complete admin report with users, books, and loans
    public byte[] generateAdminReport() throws IOException {

        try (PDDocument document = new PDDocument()) {
            PDPage currentPage = new PDPage();
            document.addPage(currentPage);
            PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);

            // Title
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.newLineAtOffset(50, 750);
            contentStream.showText("LIBRORED ADMIN REPORT");
            contentStream.endText();

            // Current date
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.newLineAtOffset(50, 720);
            contentStream.showText("Generated: " + java.time.LocalDateTime.now().toString());
            contentStream.endText();

            float yPosition = 680;
            final float bottomMargin = 50;
            final float topMargin = 750;

            // Users section
            yPosition = addSectionTitle(contentStream, "USERS", yPosition);
            List<User> users = userService.getAllUsers();

            for (User user : users) {
                String userText = String.format("ID: %d | User: %s | Email: %s | Role: %s",
                    user.getId(), user.getUsername(), user.getEmail(),
                    user.getRole().name().replace("ROLE_", ""));

                // Check if we need a new page
                if (yPosition < bottomMargin + 30) {
                    contentStream.close();
                    currentPage = new PDPage();
                    document.addPage(currentPage);
                    contentStream = new PDPageContentStream(document, currentPage);
                    yPosition = topMargin;
                }

                yPosition = addTextLine(contentStream, userText, yPosition);
            }

            yPosition -= 20;

            // Books section
            if (yPosition < bottomMargin + 50) {
                contentStream.close();
                currentPage = new PDPage();
                document.addPage(currentPage);
                contentStream = new PDPageContentStream(document, currentPage);
                yPosition = topMargin;
            }

            yPosition = addSectionTitle(contentStream, "BOOKS", yPosition);
            List<Book> books = bookService.getAllBooks();

            for (Book book : books) {
                String bookText = String.format("ID: %d | Title: %s | Author: %s | Genre: %s",
                    book.getId(), book.getTitle(), book.getAuthor(), book.getGenre().toString());

                // Check if we need a new page
                if (yPosition < bottomMargin + 30) {
                    contentStream.close();
                    currentPage = new PDPage();
                    document.addPage(currentPage);
                    contentStream = new PDPageContentStream(document, currentPage);
                    yPosition = topMargin;
                }

                yPosition = addTextLine(contentStream, bookText, yPosition);
            }

            yPosition -= 20;

            // Loans section
            if (yPosition < bottomMargin + 50) {
                contentStream.close();
                currentPage = new PDPage();
                document.addPage(currentPage);
                contentStream = new PDPageContentStream(document, currentPage);
                yPosition = topMargin;
            }

            yPosition = addSectionTitle(contentStream, "LOANS", yPosition);
            List<Loan> loans = loanService.getAllLoans();

            for (Loan loan : loans) {
                String loanText = String.format("ID: %d | Book: %s | Borrower: %s | Start: %s | End: %s",
                    loan.getId(),
                    loan.getBook() != null ? loan.getBook().getTitle() : "N/A",
                    loan.getBorrower() != null ? loan.getBorrower().getUsername() : "N/A",
                    loan.getStartDate() != null ? loan.getStartDate().toString() : "N/A",
                    loan.getEndDate() != null ? loan.getEndDate().toString() : "N/A");

                // Check if we need a new page
                if (yPosition < bottomMargin + 30) {
                    contentStream.close();
                    currentPage = new PDPage();
                    document.addPage(currentPage);
                    contentStream = new PDPageContentStream(document, currentPage);
                    yPosition = topMargin;
                }

                yPosition = addTextLine(contentStream, loanText, yPosition);
            }

            contentStream.close();

            // Convert to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            byte[] pdfBytes = baos.toByteArray();

            return pdfBytes;

        } catch (Exception e) {
            throw new IOException("Failed to generate admin report", e);
        }
    }

    private float addSectionTitle(PDPageContentStream contentStream, String title, float yPosition) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
        contentStream.newLineAtOffset(50, yPosition);
        contentStream.showText(title);
        contentStream.endText();
        return yPosition - 25;
    }

    private float addTextLine(PDPageContentStream contentStream, String text, float yPosition) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 10);
        contentStream.newLineAtOffset(50, yPosition);
        contentStream.showText(text);
        contentStream.endText();
        return yPosition - 15;
    }

    // Generate a simple text report
    public String generateTextReport() {
        StringBuilder report = new StringBuilder();
        report.append("LIBRORED ADMIN REPORT\n");
        report.append("====================\n\n");
        report.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");

        report.append("USERS:\n");
        userService.getAllUsers().forEach(user -> {
            report.append("- ID: ").append(user.getId())
                  .append(", User: ").append(user.getUsername())
                  .append(", Email: ").append(user.getEmail())
                  .append(", Role: ").append(user.getRole().name().replace("ROLE_", ""))
                  .append("\n");
        });

        return report.toString();
    }


}