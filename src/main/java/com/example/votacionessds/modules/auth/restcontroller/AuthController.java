package com.example.votacionessds.modules.auth.restcontroller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.votacionessds.modules.auth.dto.ChangePasswordRequest;
import com.example.votacionessds.modules.auth.dto.ForgotPasswordRequest;
import com.example.votacionessds.modules.auth.dto.LoginRequest;
import com.example.votacionessds.modules.auth.dto.LoginResponse;
import com.example.votacionessds.modules.auth.dto.RefreshTokenRequest;
import com.example.votacionessds.modules.auth.dto.RefreshTokenResponse;
import com.example.votacionessds.modules.auth.dto.ResetPasswordRequest;
import com.example.votacionessds.modules.auth.dto.UserResponse;
import com.example.votacionessds.modules.auth.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody final LoginRequest request, HttpServletRequest httpRequest) {
        //System.out.println("Login request: " + request);
        String ipSolicitante = httpRequest.getRemoteAddr();
        String appSolicitante = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(request, ipSolicitante, appSolicitante));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Validated @RequestBody final RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.refreshToken(request, ip, ua));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Validated @RequestBody final RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody final ChangePasswordRequest request, @RequestParam final UUID userId) {
        authService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Validated @RequestBody final ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Validated @RequestBody final ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@RequestParam final String username) {
        return ResponseEntity.ok(authService.me(username));
    }
}
