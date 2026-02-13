package es.codeurjc13.librored.security;

import es.codeurjc13.librored.model.User;
import es.codeurjc13.librored.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class RepositoryUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

/*    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("🔍 Loading user for authentication: " + email);

        User user = userRepository.findByEmail(email)  // Fetch by email instead of username
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail()) // Use email for authentication
                .password(user.getEncodedPassword()) // Use encoded password
                .roles(user.getRole().name().replace("ROLE_", "")) // Remove "ROLE_" prefix
                .build();
    }*/

/*    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
                user.getId().toString(),  // ✅ Store User ID as username instead of email
                user.getEncodedPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }*/

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),  // ✅ Store email as username
                user.getEncodedPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }

}