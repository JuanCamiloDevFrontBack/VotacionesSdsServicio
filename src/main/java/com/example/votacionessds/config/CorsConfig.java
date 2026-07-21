package com.example.votacionessds.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


@Configuration
@EnableWebSecurity
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        System.out.println("CorsConfig: corsConfigurationSource");
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
