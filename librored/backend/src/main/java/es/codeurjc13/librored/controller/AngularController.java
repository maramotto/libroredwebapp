package es.codeurjc13.librored.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AngularController {

    // Serve index.html for Angular routes, but let static resources be handled normally
    @GetMapping({"/new", "/new/", "/new/books", "/new/books/**", "/new/login", "/new/register", "/new/loans", "/new/loans/**", "/new/account", "/new/my-books", "/new/my-loans", "/new/admin", "/new/admin/**"})
    public String angular() {
        return "forward:/new/index.html";
    }
}