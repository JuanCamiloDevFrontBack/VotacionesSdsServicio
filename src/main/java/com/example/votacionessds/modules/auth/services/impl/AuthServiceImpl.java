package com.example.votacionessds.modules.auth.services.impl;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.votacionessds.exceptions.ConflictException;
import com.example.votacionessds.exceptions.ErrorCode;
import com.example.votacionessds.exceptions.InvalidCredentialsException;
import com.example.votacionessds.exceptions.ResourceNotFoundException;

import com.example.votacionessds.modules.auth.dao.RoleRepository;
import com.example.votacionessds.modules.auth.dao.UserRepository;
import com.example.votacionessds.modules.auth.dto.ChangePasswordRequest;
import com.example.votacionessds.modules.auth.dto.CreateUserRequest;
import com.example.votacionessds.modules.auth.dto.ForgotPasswordRequest;
import com.example.votacionessds.modules.auth.dto.LoginRequest;
import com.example.votacionessds.modules.auth.dto.LoginResponse;
import com.example.votacionessds.modules.auth.dto.RefreshTokenRequest;
import com.example.votacionessds.modules.auth.dto.RefreshTokenResponse;
import com.example.votacionessds.modules.auth.dto.ResetPasswordRequest;
import com.example.votacionessds.modules.auth.dto.UserResponse;
import com.example.votacionessds.modules.auth.entity.RefreshToken;
import com.example.votacionessds.modules.auth.entity.Role;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.services.AuthService;
import com.example.votacionessds.modules.auth.services.RefreshTokenService;
import com.example.votacionessds.modules.auth.services.UserSessionService;
import com.example.votacionessds.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSessionService userSessionService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipSolicitante, String appSolicitante) {
        User user = userRepository.findByUsername(request.getUsername())
        .orElseThrow(() -> new InvalidCredentialsException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials"));
        
        if (!user.isAccountNonLocked()) {
            throw new InvalidCredentialsException(ErrorCode.ACCOUNT_LOCKED, "Account is locked");
        }

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
            request.getUsername(),
            request.getPassword()));
        System.out.println("Authentication: " + authentication);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // reset failed attempts
        user.setFailedAttempts(0);
        userRepository.save(user);

        org.springframework.security.core.userdetails.User principal = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        String accessToken = jwtProvider.generateAccessToken(principal);
        RefreshToken rt = refreshTokenService.createRefreshToken(user);

        // create session (placeholder)
        userSessionService.createSession(user, ipSolicitante, appSolicitante);

        return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(rt.getToken())
        .tokenType("Bearer")
        .build();
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request, String ip, String userAgent) {
        RefreshToken existing = refreshTokenService.findByToken(request.getRefreshToken())
        .orElseThrow(() -> new InvalidCredentialsException(ErrorCode.INVALID_REFRESH_TOKEN, "Invalid refresh token"));
        if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException(ErrorCode.REFRESH_TOKEN_EXPIRED, "Refresh token revoked or expired");
        }
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(existing);
        String accessToken = jwtProvider.generateAccessToken(
            new org.springframework.security.core.userdetails.User(
                existing.getUser().getUsername(),
                existing.getUser().getPassword(),
                existing.getUser().getRoles() == null ? java.util.List.of() :
                existing.getUser().getRoles().stream().map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority(r.getCode()))
                .collect(Collectors.toList())));
        return RefreshTokenResponse.builder().accessToken(accessToken).refreshToken(rotated.getToken()).build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.findByToken(refreshToken).ifPresent(rt -> {
            refreshTokenService.revoke(rt);
        });
    }

    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException(ErrorCode.INVALID_CREDENTIALS, "Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // generate password reset token and send email - placeholder
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            // create PasswordResetToken entity and send email (omitted)
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // validate token and set new password - placeholder
    }

    @Override
    public UserResponse me(String username) {
        User u = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .roles(u.getRoles() == null ? Set.of("VOTANTE") : u.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toSet()))
                .createdAt(u.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException(ErrorCode.USERNAME_EXISTS, "Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException(ErrorCode.EMAIL_EXISTS, "Email already exists");
        }

        String roleCode = request.getRoleCode() != null && !request.getRoleCode().isBlank()
                ? request.getRoleCode()
                : "VOTANTE";
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ROLE_NOT_FOUND, "Role not found: " + roleCode));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .provider("LOCAL")
                .enabled(true)
                .accountLocked(false)
                .failedAttempts(0)
                .createdAt(Instant.now())
                .passwordChangedAt(Instant.now())
                .roles(Set.of(role))
                .build();

        User saved = userRepository.save(user);

        return UserResponse.builder()
                .id(saved.getId())
                .username(saved.getUsername())
                .email(saved.getEmail())
                .roles(Set.of(role.getCode()))
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
