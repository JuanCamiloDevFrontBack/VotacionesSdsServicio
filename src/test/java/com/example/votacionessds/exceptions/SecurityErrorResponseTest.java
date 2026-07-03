package com.example.votacionessds.exceptions;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import com.example.votacionessds.config.JwtAccessDeniedHandler;
import com.example.votacionessds.config.JwtAuthenticationEntryPoint;

@ExtendWith(MockitoExtension.class)
class SecurityErrorResponseTest {

    @Mock
    private ApiErrorResponseWriter errorResponseWriter;

    @InjectMocks
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @InjectMocks
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Test
    void authenticationEntryPoint_writesUnauthorizedJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(request, response, new InsufficientAuthenticationException("test"));

        verify(errorResponseWriter).write(eq(response), eq(HttpStatus.UNAUTHORIZED), eq(ErrorCode.UNAUTHORIZED),
                eq("Unauthorized"), eq("/api/auth/me"));
    }

    @Test
    void accessDeniedHandler_writesForbiddenJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/logout");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("test"));

        verify(errorResponseWriter).write(eq(response), eq(HttpStatus.FORBIDDEN), eq(ErrorCode.ACCESS_DENIED),
                eq("Access denied"), eq("/api/auth/logout"));
    }
}
