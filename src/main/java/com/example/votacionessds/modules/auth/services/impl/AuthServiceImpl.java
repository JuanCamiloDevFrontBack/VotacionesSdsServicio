package com.example.votacionessds.modules.auth.services.impl;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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
import com.example.votacionessds.modules.auth.dto.UpdateUserTestRequest;
import com.example.votacionessds.modules.auth.dto.UpdateUsernameByRefreshTokenTestRequest;
import com.example.votacionessds.modules.auth.dto.UserResponse;
import com.example.votacionessds.modules.auth.entity.RefreshToken;
import com.example.votacionessds.modules.auth.entity.Role;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.entity.UserSession;
import com.example.votacionessds.modules.auth.services.AuthService;
import com.example.votacionessds.modules.auth.services.LoginAuditService;
import com.example.votacionessds.modules.auth.services.RefreshTokenService;
import com.example.votacionessds.modules.auth.services.RefreshTokenService.IssuedRefreshToken;
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
    private final LoginAuditService loginAuditService;
    private final UserSessionService userSessionService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ipSolicitante, String appSolicitante) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            loginAuditService.recordFailure(request.getEmail(), ipSolicitante, appSolicitante);
            throw new InvalidCredentialsException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }

        if (!user.isLoginAllowed()) {
            loginAuditService.recordFailure(user.getEmail(), ipSolicitante, appSolicitante);
            throw new InvalidCredentialsException(ErrorCode.ACCOUNT_LOCKED, "Account is locked");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));
            System.out.println("--try cach {authentication: --}" + authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            user.setFailedLoginAttempts((short) 0);
            user.setLockedUntil(null);
            userRepository.save(user);

            userSessionService.revokeAllActiveForUser(user.getId());
            refreshTokenService.revokeAllForUser(user);

            org.springframework.security.core.userdetails.User principal =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
            IssuedRefreshToken issued = refreshTokenService.issue(user, ipSolicitante, appSolicitante);
            UserSession session = userSessionService.createSession(
                    user,
                    issued.entity().getFamilyId(),
                    ipSolicitante,
                    appSolicitante,
                    issued.entity().getExpiresAt());
            String accessToken = jwtProvider.generateAccessToken(principal, session.getId());
            loginAuditService.recordSuccess(user, ipSolicitante, appSolicitante);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(issued.rawToken())
                    .tokenType("Bearer")
                    .sessionId(session.getId())
                    .build();
        } catch (BadCredentialsException ex) {
            user.setFailedLoginAttempts((short) (user.getFailedLoginAttempts() + 1));
            userRepository.save(user);
            loginAuditService.recordFailure(user.getEmail(), ipSolicitante, appSolicitante);
            throw new InvalidCredentialsException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request, String ip, String userAgent) {
        RefreshToken existing = refreshTokenService.resolve(request.getRefreshToken())
                .orElseThrow(() -> new InvalidCredentialsException(
                        ErrorCode.INVALID_REFRESH_TOKEN,
                        "Invalid refresh token"));

        IssuedRefreshToken rotated = refreshTokenService.rotate(existing, ip, userAgent);
        User user = existing.getUser();
        userSessionService.touchByFamilyId(existing.getFamilyId());
        UUID sessionId = userSessionService.findActiveByFamilyId(existing.getFamilyId())
                .map(UserSession::getId)
                .orElseThrow(() -> new InvalidCredentialsException(
                        ErrorCode.SESSION_REVOKED,
                        "Session is no longer active"));
        String accessToken = jwtProvider.generateAccessToken(toSpringUser(user), sessionId);

        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rotated.rawToken())
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.resolve(refreshToken).ifPresent(token -> {
            userSessionService.revokeByFamilyId(token.getFamilyId());
            refreshTokenService.revokeFamily(token.getFamilyId());
        });
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(ErrorCode.INVALID_CREDENTIALS, "Current password does not match");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);
        userSessionService.revokeAllActiveForUser(userId);
        refreshTokenService.revokeAllForUser(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Sin tabla de reset de contraseña en el esquema actual: no revelar si el email existe.
        userRepository.findByEmail(request.getEmail());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        throw new UnsupportedOperationException(
                "Password reset is not available until a reset-token table is added to the schema");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
        return toUserResponse(user);
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

        Short roleId = request.getRoleId();
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.ROLE_NOT_FOUND,
                        "Role not found"));

        Instant now = Instant.now();
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .emailVerified(false)
                .accountLocked(false)
                .failedLoginAttempts((short) 0)
                .passwordChangedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .roles(Set.of(role))
                .build();

        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUserTest(String accessToken, UpdateUserTestRequest request) {
        // de prueba — se quitará después
        if (accessToken == null || accessToken.isBlank() || !jwtProvider.validateToken(accessToken)) {
            throw new InvalidCredentialsException(ErrorCode.UNAUTHORIZED, "Invalid or missing access token");
        }

        String email = jwtProvider.getUsernameFromToken(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (!user.getUsername().equals(request.getUsername())
                && userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException(ErrorCode.USERNAME_EXISTS, "Username already exists");
        }
        if (!user.getEmail().equals(request.getEmail())
                && userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ConflictException(ErrorCode.EMAIL_EXISTS, "Email already exists");
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUsernameByRefreshTokenTest(UpdateUsernameByRefreshTokenTestRequest request) {
        // de prueba — se quitará después
        RefreshToken refreshToken = refreshTokenService.resolve(request.getRefreshToken())
                .orElseThrow(() -> new InvalidCredentialsException(
                        ErrorCode.INVALID_REFRESH_TOKEN,
                        "Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidCredentialsException(
                    ErrorCode.INVALID_REFRESH_TOKEN,
                    "Invalid refresh token");
        }
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidCredentialsException(
                    ErrorCode.REFRESH_TOKEN_EXPIRED,
                    "Refresh token expired");
        }
        if (!refreshToken.getUser().getId().equals(request.getUserId())) {
            throw new InvalidCredentialsException(
                    ErrorCode.ACCESS_DENIED,
                    "Refresh token does not belong to this user");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (!user.getUsername().equals(request.getUsername())
                && userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException(ErrorCode.USERNAME_EXISTS, "Username already exists");
        }

        user.setUsername(request.getUsername());
        User saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    private org.springframework.security.core.userdetails.User toSpringUser(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.getRoles() == null ? java.util.List.of()
                        : user.getRoles().stream()
                                .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.getName()))
                                .collect(Collectors.toList()));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles((user.getRoles() == null || user.getRoles().isEmpty())
                        ? Set.of("ROLE_VOTE")
                        : user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
