package es.codeurjc13.librored.controller;

import es.codeurjc13.librored.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"https://localhost:8443"}, allowCredentials = "true")
public class AdminRestController {

    private final ReportService reportService;

    public AdminRestController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/download-report")
    @PreAuthorize("hasRole('ADMIN')")
    public void downloadReport(HttpServletResponse response) throws IOException {
        try {
            // Generate PDF report
            byte[] pdfData = reportService.generateAdminReport();

            // Validate PDF data
            if (pdfData == null || pdfData.length == 0) {
                throw new RuntimeException("Generated PDF is empty");
            }

            // Set HTTP response headers
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"Admin_Report.pdf\"");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setContentLength(pdfData.length);

            // Write PDF data to response
            response.getOutputStream().write(pdfData);
            response.getOutputStream().flush();

        } catch (Exception e) {
            response.reset();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Failed to generate report: " + e.getMessage() + "\"}");
        }
    }
}