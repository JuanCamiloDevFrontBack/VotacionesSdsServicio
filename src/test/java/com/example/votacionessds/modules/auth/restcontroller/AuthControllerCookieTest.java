package com.example.votacionessds.modules.auth.restcontroller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.votacionessds.modules.auth.dto.LoginRequest;
import com.example.votacionessds.modules.auth.dto.LoginResponse;
import com.example.votacionessds.modules.auth.dto.RefreshTokenResponse;
import com.example.votacionessds.modules.auth.services.AuthService;
import com.example.votacionessds.modules.auth.services.RefreshTokenCookieService;
import com.example.votacionessds.modules.auth.services.RefreshTokenCookieServiceImpl;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.servlet.http.Cookie;

class AuthControllerCookieTest {

    private AuthService authService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        RefreshTokenCookieService cookieService = new RefreshTokenCookieServiceImpl(
                "refresh_token",
                true,
                "None",
                604800000L,
                "/system-votaciones-sds");
        controller = new AuthController(authService, cookieService);
    }

    @Test
    void login_setsRefreshTokenCookieAndKeepsItOutOfJsonContract() throws NoSuchFieldException {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("user@example.com")
                .password("password")
                .build();
        MockHttpServletRequest servletRequest = request();
        when(authService.login(loginRequest, "127.0.0.1", "JUnit"))
                .thenReturn(LoginResponse.builder()
                        .accessToken("access-token")
                        .refreshToken("refresh-token")
                        .tokenType("Bearer")
                        .build());

        ResponseEntity<LoginResponse> response = controller.login(loginRequest, servletRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=refresh-token", "HttpOnly", "Secure", "SameSite=None");
        assertThat(LoginResponse.class.getDeclaredField("refreshToken").getAnnotation(JsonIgnore.class))
                .isNotNull();
    }

    @Test
    void refresh_readsCookieAndSetsRotatedCookie() {
        MockHttpServletRequest request = request();
        request.setCookies(new Cookie("refresh_token", "old-refresh-token"));
        when(authService.refreshToken("old-refresh-token", "127.0.0.1", "JUnit"))
                .thenReturn(RefreshTokenResponse.builder()
                        .accessToken("new-access-token")
                        .refreshToken("new-refresh-token")
                        .tokenType("Bearer")
                        .build());

        ResponseEntity<RefreshTokenResponse> response = controller.refreshToken(request);

        verify(authService).refreshToken("old-refresh-token", "127.0.0.1", "JUnit");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=new-refresh-token", "HttpOnly");
        assertThat(response.getBody().getAccessToken()).isEqualTo("new-access-token");
    }

    @Test
    void logout_revokesTokenAndExpiresCookie() {
        MockHttpServletRequest request = request();
        request.setCookies(new Cookie("refresh_token", "refresh-token"));

        ResponseEntity<Void> response = controller.logout(request);

        verify(authService).logout("refresh-token");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=", "Max-Age=0", "HttpOnly");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        return request;
    }
}
