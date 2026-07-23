package com.example.votacionessds.modules.auth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.votacionessds.exceptions.InvalidCredentialsException;
import com.example.votacionessds.security.RefreshTokenCookieService;
import com.example.votacionessds.security.RefreshTokenCookieServiceImpl;

import jakarta.servlet.http.Cookie;

class RefreshTokenCookieServiceTest {

    private RefreshTokenCookieService cookieService;

    @BeforeEach
    void setUp() {
        cookieService = new RefreshTokenCookieServiceImpl(
                "refresh_token",
                true,
                "None",
                604800000L,
                "/system-votaciones-sds");
    }

    @Test
    void createCookie_usesSecureHttpOnlyCrossSiteSettings() {
        String setCookie = cookieService.createCookie("secret-token").toString();

        assertThat(setCookie)
                .contains("refresh_token=secret-token")
                .contains("Path=/system-votaciones-sds/api/auth")
                .contains("Max-Age=604800")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=None");
    }

    @Test
    void getRefreshToken_readsConfiguredCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "secret-token"));

        assertThat(cookieService.getRefreshToken(request)).isEqualTo("secret-token");
    }

    @Test
    void getRefreshToken_rejectsMissingCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> cookieService.getRefreshToken(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Refresh token cookie is missing");
    }

    @Test
    void clearCookie_expiresCookieUsingSameAttributes() {
        String setCookie = cookieService.clearCookie().toString();

        assertThat(setCookie)
                .contains("refresh_token=")
                .contains("Path=/system-votaciones-sds/api/auth")
                .contains("Max-Age=0")
                .contains("Secure")
                .contains("HttpOnly")
                .contains("SameSite=None");
    }
}
