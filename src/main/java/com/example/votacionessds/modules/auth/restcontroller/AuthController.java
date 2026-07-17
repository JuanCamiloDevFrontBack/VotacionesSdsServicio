package com.example.votacionessds.modules.auth.restcontroller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;     
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
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
import com.example.votacionessds.modules.auth.dto.RefreshTokenResponse;
import com.example.votacionessds.modules.auth.dto.ResetPasswordRequest;
import com.example.votacionessds.modules.auth.dto.UpdateUserTestRequest;
import com.example.votacionessds.modules.auth.dto.UpdateUsernameByRefreshTokenTestRequest;
import com.example.votacionessds.modules.auth.dto.UserResponse;
import com.example.votacionessds.modules.auth.services.AuthService;
import com.example.votacionessds.modules.auth.services.RefreshTokenCookieService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody final LoginRequest request, HttpServletRequest httpRequest) {
        String ipSolicitante = httpRequest.getRemoteAddr();
        String appSolicitante = httpRequest.getHeader("User-Agent");
        LoginResponse response = authService.login(request, ipSolicitante, appSolicitante);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookieService.createCookie(response.getRefreshToken()).toString())
                .body(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @RequestHeader("X-Requested-With") final String requestedWith,
            HttpServletRequest httpRequest) {
        validateCsrfHeader(requestedWith);
        String refreshToken = refreshTokenCookieService.getRefreshToken(httpRequest);
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        RefreshTokenResponse response = authService.refreshToken(refreshToken, ip, ua);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        refreshTokenCookieService.createCookie(response.getRefreshToken()).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("X-Requested-With") final String requestedWith,
            HttpServletRequest httpRequest) {
        validateCsrfHeader(requestedWith);
        authService.logout(refreshTokenCookieService.getRefreshToken(httpRequest));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clearCookie().toString())
                .build();
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
        return ResponseEntity.ok(authService.updateUserTest(authorization, request));
    }

    @PutMapping("/test-update-username")
    public ResponseEntity<UserResponse> updateUsernameByRefreshTokenTest(
            @Validated @RequestBody final UpdateUsernameByRefreshTokenTestRequest request) {
        // de prueba — se quitará después
        return ResponseEntity.ok(authService.updateUsernameByRefreshTokenTest(request));
    }

    @GetMapping("/test-users")
    public ResponseEntity<List<UserResponse>> getAllUsersTest(
            @RequestHeader(value = "Authorization", required = true) final String authorization) {
        // de prueba — se quitará después
        return ResponseEntity.ok(authService.getAllUsersTest(authorization));
    }

    private void validateCsrfHeader(String requestedWith) {
        if (!"XMLHttpRequest".equals(requestedWith)) {
            throw new AccessDeniedException("Invalid CSRF protection header");
        }
    }
}
