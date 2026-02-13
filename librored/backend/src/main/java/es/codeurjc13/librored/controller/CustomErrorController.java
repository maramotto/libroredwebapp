package es.codeurjc13.librored.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            return switch (statusCode) {
                case 404 -> "error/404";
                case 500 -> "error/500";
                case 403 -> "error/403";
                case 401 -> "error/401";
                default -> "error/default"; // Ensure this file exists
            };

        }
        return "error/404";
    }

    @GetMapping("/loginerror")
    public String loginError() {
        return "error/loginerror";
    }
}
