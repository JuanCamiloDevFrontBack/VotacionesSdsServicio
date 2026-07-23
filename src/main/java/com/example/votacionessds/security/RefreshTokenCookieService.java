package com.example.votacionessds.security;

import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletRequest;

public interface RefreshTokenCookieService {

    ResponseCookie createCookie(String refreshToken);

    String getRefreshToken(HttpServletRequest request);

    ResponseCookie clearCookie();
}
