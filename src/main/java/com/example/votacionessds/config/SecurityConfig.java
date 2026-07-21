package com.example.votacionessds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.example.votacionessds.modules.auth.services.UserSessionService;
import com.example.votacionessds.security.CustomUserDetailsService;
import com.example.votacionessds.security.JwtAuthenticationFilter;
import com.example.votacionessds.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtProvider jwtProvider;
    private final UserSessionService userSessionService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return auth.build();
    }

    /**
     * @Bean explícito (no @Component en la clase): así Spring Boot nunca lo
     * auto-registra como filtro de servlet genérico en "/*", que causaba que
     * se ejecutara dos veces por request y se saltara authorizeHttpRequests.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, userDetailsService, userSessionService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // Sin cookies de sesión de servidor ni credenciales auto-enviadas por el
                // navegador en los endpoints Bearer -> CSRF clásico no aplica ahí.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh-token").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Solo activo si el perfil NO es "prod" (ver @Profile en DevUserTestController).
                        // Aun así, público a propósito -> revisar ese archivo antes de tocar esta línea.
                        .requestMatchers(HttpMethod.POST, "/api/dev/users").permitAll()
                        .anyRequest().authenticated());

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
