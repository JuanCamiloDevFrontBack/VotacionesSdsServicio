package com.example.votacionessds.security;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.votacionessds.modules.auth.services.UserSessionService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements Filter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private final UserSessionService userSessionService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        var typeHeader = "Authorization";
        var prefixHeader = "Bearer ";
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String header = request.getHeader(typeHeader);
        String token = null;
        if (StringUtils.hasText(header) && header.startsWith(prefixHeader)) {
            token = header.substring(prefixHeader.length());
        }

        if (token != null && jwtProvider.validateToken(token)) {
            Optional<UUID> sessionId = jwtProvider.getSessionIdFromToken(token);
            if (sessionId.isPresent() && userSessionService.isActive(sessionId.get())) {
                String username = jwtProvider.getUsernameFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
