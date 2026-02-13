package es.codeurjc13.librored.security;

import es.codeurjc13.librored.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import es.codeurjc13.librored.security.jwt.JwtRequestFilter;
import es.codeurjc13.librored.security.jwt.UnauthorizedHandlerJwt;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    RepositoryUserDetailsService userDetailsService;

    @Autowired
    private UnauthorizedHandlerJwt unauthorizedHandlerJwt;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);  // Now using email instead of username
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Public API endpoints filter chain (Order 0 - Highest Priority)
    @Bean
    @Order(0)
    public SecurityFilterChain publicApiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/books/books-per-genre", "/api/loans/valid-borrowers")
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }

    // REST API Security Configuration (Order 1 - Higher Priority)
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

        http.authenticationProvider(authenticationProvider());

        http
            .securityMatcher(request -> {
                String uri = request.getRequestURI();
                return uri.startsWith("/api/") &&
                       !uri.equals("/api/books/books-per-genre") &&
                       !uri.equals("/api/loans/valid-borrowers");
            })
            .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedHandlerJwt));
        
        http
            .authorizeHttpRequests(authorize -> authorize
                    // PUBLIC API ENDPOINTS
                    .requestMatchers("/api/books", "/api/books/books-per-genre", "/api/books/**").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/swagger-ui/index.html").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll() // Actuator health checks
                    .requestMatchers("/api/auth/**").permitAll() // JWT auth endpoints

                    // Book API endpoints - SPECIFIC RULES FIRST (before general /api/v1/**)
                    .requestMatchers(HttpMethod.GET,"/api/v1/books/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.POST,"/api/v1/books").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.PUT,"/api/v1/books/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.DELETE,"/api/v1/books/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.POST,"/api/v1/books/*/cover").hasAnyRole("USER", "ADMIN")

                    // Legacy book endpoints
                    .requestMatchers(HttpMethod.POST,"/api/books/").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.PUT,"/api/books/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers(HttpMethod.DELETE,"/api/books/**").hasRole("ADMIN")

                    // PRIVATE API ENDPOINTS (P2 requirements) - AFTER specific rules
                    .requestMatchers("/api/v1/**").authenticated()
                    
                    // API access: Only logged-in users
                    .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
                    .requestMatchers("/api/users/verify-password", "/api/users/update-username", "/api/users/update-password").authenticated()

                    // Admin API endpoints
                    .requestMatchers("/api/download-report").hasRole("ADMIN")

                    // All other API endpoints require authentication
                    .requestMatchers("/api/**").authenticated()
            );
        
        // Disable Form login Authentication
        http.formLogin(formLogin -> formLogin.disable());

        // Disable CSRF protection (it is difficult to implement in REST APIs)
        http.csrf(csrf -> csrf.disable());

        // Disable Basic Authentication
        http.httpBasic(httpBasic -> httpBasic.disable());

        // Stateless session
        http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Add JWT Token filter
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Web Application Security Configuration (Order 2 - Lower Priority)  
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        
        http.authenticationProvider(authenticationProvider())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .authorizeHttpRequests(auth -> auth
                        // Public resources (CSS, JS, Images)
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // Public pages
                        .requestMatchers("/", "/login", "/register", "/error/**", "/perform_login", "/loginerror").permitAll()
                        .requestMatchers("/new/**").permitAll() // Angular SPA
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll() // Health checks
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/swagger-ui/index.html").permitAll() // Swagger UI

                        // User dashboard and protected actions
                        .requestMatchers("/users/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers("/myaccount").authenticated()
                        .requestMatchers("/api/loans/valid-borrowers").authenticated()
                        .requestMatchers("/users/edit/{id}").access((authentication, request) -> {
                            boolean isAdmin = authentication.get().getAuthorities().stream()
                                    .anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN") || role.getAuthority().equals(User.Role.ROLE_ADMIN.name()));

                            boolean isSelf = authentication.get().getName().equals(request.getRequest().getServletPath().split("/")[3]);

                            return new AuthorizationDecision(isAdmin || isSelf);
                        })

                        .requestMatchers("/books").authenticated()
                        .requestMatchers("/loans").authenticated()
                        .requestMatchers("/loans/**").authenticated()
                        .requestMatchers("/recommendations").authenticated()

                        // Admin-only pages
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")

                        // Any other request requires authentication
                        .anyRequest().authenticated())
                .formLogin(formLogin -> formLogin
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .failureUrl("/login?error=true")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendRedirect("/login");
                        }));

        return http.build();
    }

}