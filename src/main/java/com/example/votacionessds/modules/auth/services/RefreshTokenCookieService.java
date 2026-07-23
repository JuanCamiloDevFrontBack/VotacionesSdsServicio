package com.example.votacionessds.modules.auth.services;

import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.HttpServletRequest;

public interface RefreshTokenCookieService {

    ResponseCookie createCookie(String refreshToken);

    String getRefreshToken(HttpServletRequest request);

    ResponseCookie clearCookie();
}
