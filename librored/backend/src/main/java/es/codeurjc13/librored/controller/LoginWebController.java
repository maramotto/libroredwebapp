package es.codeurjc13.librored.controller;

import es.codeurjc13.librored.model.User;
import es.codeurjc13.librored.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class LoginWebController {

    private final UserService userService;

    public LoginWebController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "successMessage", required = false) String successMessage,
            @RequestParam(value = "message", required = false) String message,
            Model model) {

        if (error != null) {
            model.addAttribute("loginError", "Invalid username or password.");
        }

        if (successMessage != null) {
            model.addAttribute("successMessage", successMessage);
        }
        
        if (message != null) {
            model.addAttribute("successMessage", message);
        }
        
        if ("registered".equals(error)) {
            model.addAttribute("successMessage", "Registration successful! Please log in.");
        }

        return "login"; // Ensure login.html displays the message
    }


    // Show the registration form
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    // Process the registration form
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user, 
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        
        try {
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}
