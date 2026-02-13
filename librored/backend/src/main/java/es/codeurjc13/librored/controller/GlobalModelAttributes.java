package es.codeurjc13.librored.controller;

import es.codeurjc13.librored.model.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("logged")
    public boolean isLogged() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String principalName = authentication.getName();
        return authentication.isAuthenticated() && !"anonymousUser".equals(principalName);
    }


    @ModelAttribute("admin")
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(User.Role.ROLE_ADMIN.name()));
        }
        return false;
    }
}
