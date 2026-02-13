package es.codeurjc13.librored.security;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for LibroRed REST API
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LibroRed API")
                        .version("1.0")
                        .description("REST API for LibroRed - Web Application for Book Lending Between Individuals")
                        .contact(new Contact()
                                .name("Team 13")
                                .email("am.juradoc@alumnos.urjc.es")
                                .url("https://github.com/medinaymedia"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .servers(List.of(
                        new Server()
                                .url("https://localhost:8443")
                                .description("Development server")));
    }
}