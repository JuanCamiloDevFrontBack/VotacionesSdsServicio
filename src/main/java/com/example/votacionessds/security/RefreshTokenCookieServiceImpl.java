package com.example.votacionessds.security;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.votacionessds.exceptions.ErrorCode;
import com.example.votacionessds.exceptions.InvalidCredentialsException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RefreshTokenCookieServiceImpl implements RefreshTokenCookieService {

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final Duration maxAge;
    private final String path;

    public RefreshTokenCookieServiceImpl(
            @Value("${security.refresh-cookie.name:refresh_token}") String cookieName,
            @Value("${security.refresh-cookie.secure:true}") boolean secure,
            @Value("${security.refresh-cookie.same-site:None}") String sameSite,
            @Value("${security.refresh-token.expiration-time:604800000}") long expirationTimeMs,
            @Value("${server.servlet.context-path}") String contextPath) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAge = Duration.ofMillis(expirationTimeMs);
        this.path = normalizeContextPath(contextPath) + "/api/auth";
    }

    @Override
    public ResponseCookie createCookie(String refreshToken) {
        return baseCookie(refreshToken)
                .maxAge(maxAge)
                .build();
    }

    @Override
    public String getRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw missingRefreshToken();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseThrow(this::missingRefreshToken);
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path);
    }

    @Override
    public ResponseCookie clearCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private InvalidCredentialsException missingRefreshToken() {
        return new InvalidCredentialsException(
                ErrorCode.INVALID_REFRESH_TOKEN,
                "Refresh token cookie is missing");
    }

    private static String normalizeContextPath(String contextPath) {
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            return "";
        }
        return contextPath.endsWith("/")
                ? contextPath.substring(0, contextPath.length() - 1)
                : contextPath;
    }
}
