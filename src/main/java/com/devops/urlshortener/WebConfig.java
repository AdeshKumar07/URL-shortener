package com.devops.urlshortener;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Allow the static UI to be opened via VS Code Live Server (typically :5500)
        // while the Spring Boot API runs on :8080.
        registry.addMapping("/shorten")
                .allowedOrigins("http://127.0.0.1:5500", "http://localhost:5500")
                .allowedMethods("POST", "OPTIONS")
                .allowedHeaders("*");

        registry.addMapping("/health")
                .allowedOrigins("http://127.0.0.1:5500", "http://localhost:5500")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");
    }
}
