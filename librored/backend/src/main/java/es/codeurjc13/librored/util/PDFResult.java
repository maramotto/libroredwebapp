package es.codeurjc13.librored.util;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

/**
 * Helper class for returning pagination results from PDF generation methods
 */
public class PDFResult {
    public PDPageContentStream contentStream;
    public PDPage currentPage;
    public int y;

    public PDFResult(PDPageContentStream contentStream, PDPage currentPage, int y) {
        this.contentStream = contentStream;
        this.currentPage = currentPage;
        this.y = y;
    }
}