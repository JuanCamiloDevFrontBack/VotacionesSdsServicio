package com.example.votacionessds.modules.auth.restcontroller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.votacionessds.modules.auth.dto.SessionResponse;
import com.example.votacionessds.modules.auth.services.RefreshTokenService;
import com.example.votacionessds.modules.auth.services.UserSessionService;
import com.example.votacionessds.security.AuthenticatedUserProvider;
import com.example.votacionessds.security.JwtProvider;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/sessions")
@RequiredArgsConstructor
public class SessionController {

    /*private final UserSessionService userSessionService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final JwtProvider jwtProvider;*/

    /*@GetMapping
    public ResponseEntity<List<SessionResponse>> listSessions(
            @RequestHeader(value = "Authorization", required = false) final String authorization) {
        UUID userId = authenticatedUserProvider.getCurrentUserId();
        UUID currentSessionId = extractSessionId(authorization).orElse(null);
        return ResponseEntity.ok(userSessionService.listActiveForUser(userId, currentSessionId));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable final UUID sessionId) {
        UUID userId = authenticatedUserProvider.getCurrentUserId();
        userSessionService.revokeSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> revokeAllSessions() {
        var user = authenticatedUserProvider.getCurrentUser();
        userSessionService.revokeAllActiveForUser(user.getId());
        refreshTokenService.revokeAllForUser(user);
        return ResponseEntity.noContent().build();
    }

    private Optional<UUID> extractSessionId(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorization.substring(7);
        if (!jwtProvider.validateToken(token)) {
            return Optional.empty();
        }
        return jwtProvider.getSessionIdFromToken(token);
    }*/
}
