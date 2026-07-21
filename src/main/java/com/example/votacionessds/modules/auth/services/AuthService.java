package com.example.votacionessds.modules.auth.services;

import com.example.votacionessds.modules.auth.dto.LoginRequest;
import com.example.votacionessds.modules.auth.dto.LoginResponse;
import com.example.votacionessds.modules.auth.dto.RefreshTokenResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request, String ip, String userAgent);
    RefreshTokenResponse refreshToken(String refreshToken, String ip, String userAgent);
    void logout(String refreshToken);
}
