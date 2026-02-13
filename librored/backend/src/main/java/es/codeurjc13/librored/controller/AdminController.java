package es.codeurjc13.librored.controller;

import es.codeurjc13.librored.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;


@Controller
@RequestMapping("/admin")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"}, allowCredentials = "true")
public class AdminController {

    private final ReportService reportService;

    public AdminController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String adminDashboard() {
        return "admin";
    }



    @GetMapping("/download-report-text")
    @PreAuthorize("hasRole('ADMIN')")
    public void downloadTextReport(HttpServletResponse response) throws IOException {
        try {
            // Generate text report using service
            String reportContent = reportService.generateTextReport();
            byte[] textData = reportContent.getBytes("UTF-8");

            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=\"Admin_Report.txt\"");
            response.setContentLength(textData.length);
            response.getOutputStream().write(textData);
            response.getOutputStream().flush();

        } catch (Exception e) {
            response.setStatus(500);
        }
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
