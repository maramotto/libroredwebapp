package es.codeurjc13.librored.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

/**
 * Helper class for managing PDF page creation and layout
 */
public class PDFPageHelper {
    private final PDDocument document;
    private static final int TOP_MARGIN = 750;
    private static final int BOTTOM_MARGIN = 50;
    private static final int LEFT_MARGIN = 50;
    private static final int RIGHT_MARGIN = 50;
    private static final int LINE_HEIGHT = 15;
    private static final int PAGE_WIDTH = 595; // A4 width in points
    private static final int MAX_LINE_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN;

    public PDFPageHelper(PDDocument document) {
        this.document = document;
    }

    public PDPage createNewPage() {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return page;
    }

    public PDPageContentStream createContentStream(PDPage page) throws IOException {
        return new PDPageContentStream(document, page);
    }

    public int addTitle(PDPageContentStream contentStream, String title) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
        contentStream.beginText();
        contentStream.newLineAtOffset(200, TOP_MARGIN);
        contentStream.showText(title);
        contentStream.endText();
        return TOP_MARGIN - 50;
    }

    public int addSectionHeader(PDPageContentStream contentStream, String title, int y) throws IOException {
        if (y < BOTTOM_MARGIN + 30) {
            return -1; // Signal that new page is needed
        }
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
        contentStream.newLineAtOffset(LEFT_MARGIN, y);
        contentStream.showText(title);
        contentStream.endText();
        return y - 25;
    }

    public boolean needsNewPage(int y) {
        return y < BOTTOM_MARGIN + LINE_HEIGHT;
    }

    public String truncateText(String text, PDType1Font font, float fontSize) {
        try {
            float textWidth = font.getStringWidth(text) / 1000 * fontSize;
            if (textWidth <= MAX_LINE_WIDTH) {
                return text;
            }
            
            // Binary search to find maximum characters that fit
            int left = 0;
            int right = text.length();
            String result = text;
            
            while (left < right) {
                int mid = (left + right + 1) / 2;
                String candidate = text.substring(0, mid) + "...";
                float candidateWidth = font.getStringWidth(candidate) / 1000 * fontSize;
                
                if (candidateWidth <= MAX_LINE_WIDTH) {
                    left = mid;
                    result = candidate;
                } else {
                    right = mid - 1;
                }
            }
            return result;
        } catch (IOException e) {
            // Fallback to simple truncation
            return text.length() > 80 ? text.substring(0, 77) + "..." : text;
        }
    }

    // Getters for constants
    public static int getTopMargin() { return TOP_MARGIN; }
    public static int getBottomMargin() { return BOTTOM_MARGIN; }
    public static int getLeftMargin() { return LEFT_MARGIN; }
    public static int getLineHeight() { return LINE_HEIGHT; }
}