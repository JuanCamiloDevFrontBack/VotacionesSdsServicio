package com.example.votacionessds.modules.auth.restcontroller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
import com.example.votacionessds.modules.auth.dto.UpdateUserTestRequest;
import com.example.votacionessds.modules.auth.dto.UpdateUsernameByRefreshTokenTestRequest;
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
        String ipSolicitante = httpRequest.getRemoteAddr();
        String appSolicitante = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(request, ipSolicitante, appSolicitante));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Validated @RequestBody final RefreshTokenRequest request, HttpServletRequest httpRequest) {
        System.out.println("endpoint request refreshtoken: " + request);
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
    public ResponseEntity<Void> changePassword(@Validated @RequestBody final ChangePasswordRequest request, @RequestParam final UUID userId) {
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

    @PutMapping("/test-update-user")
    public ResponseEntity<UserResponse> updateUserTest(
            @RequestHeader(value = "Authorization", required = false) final String authorization,
            @Validated @RequestBody final UpdateUserTestRequest request) {
        // de prueba — se quitará después
        String token = null;
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        return ResponseEntity.ok(authService.updateUserTest(token, request));
    }

    @PutMapping("/test-update-username")
    public ResponseEntity<UserResponse> updateUsernameByRefreshTokenTest(
            @Validated @RequestBody final UpdateUsernameByRefreshTokenTestRequest request) {
        // de prueba — se quitará después
        return ResponseEntity.ok(authService.updateUsernameByRefreshTokenTest(request));
    }
}
