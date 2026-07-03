package com.example.votacionessds.modules.auth.services;

import com.example.votacionessds.modules.auth.dto.*;

import java.util.UUID;

public interface AuthService {
    LoginResponse login(LoginRequest request, String ip, String userAgent);
    RefreshTokenResponse refreshToken(RefreshTokenRequest request, String ip, String userAgent);
    void logout(String refreshToken);
    void changePassword(UUID userId, ChangePasswordRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    UserResponse me(String username);

    UserResponse createUser(CreateUserRequest request);
}
