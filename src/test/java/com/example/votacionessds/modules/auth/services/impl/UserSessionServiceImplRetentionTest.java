package com.example.votacionessds.modules.auth.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.votacionessds.modules.auth.dao.RefreshTokenRepository;
import com.example.votacionessds.modules.auth.dao.UserSessionRepository;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.entity.UserSession;
import com.example.votacionessds.security.UserSessionServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceImplRetentionTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserSessionServiceImpl userSessionService;

    private User user;
    private UUID userId;
    private UUID familyId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userSessionService, "maxUserSessions", 3);
        userId = UUID.randomUUID();
        familyId = UUID.randomUUID();
        user = User.builder().id(userId).email("user@example.com").username("user").build();
    }

    @Test
    void createSession_prunesExcessSessionsAndRefreshTokens() {
        UUID excessFamily = UUID.randomUUID();
        UserSession saved = UserSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .familyId(familyId)
                .createdAt(Instant.now())
                .lastSeenAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        when(userSessionRepository.save(any(UserSession.class))).thenReturn(saved);
        when(userSessionRepository.findFamilyIdsOlderThanKeep(userId, 3)).thenReturn(List.of(excessFamily));
        when(userSessionRepository.deleteOlderThanKeep(userId, 3)).thenReturn(1);

        UserSession result = userSessionService.createSession(
                user, familyId, "127.0.0.1", "JUnit", Instant.now().plusSeconds(60));

        assertThat(result.getId()).isEqualTo(saved.getId());
        verify(refreshTokenRepository).deleteByFamilyIdIn(List.of(excessFamily));
        verify(userSessionRepository).deleteOlderThanKeep(userId, 3);
    }

    @Test
    void createSession_skipsRefreshTokenDeleteWhenNothingToPrune() {
        UserSession saved = UserSession.builder()
                .id(UUID.randomUUID())
                .user(user)
                .familyId(familyId)
                .createdAt(Instant.now())
                .lastSeenAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        when(userSessionRepository.save(any(UserSession.class))).thenReturn(saved);
        when(userSessionRepository.findFamilyIdsOlderThanKeep(userId, 3)).thenReturn(List.of());
        when(userSessionRepository.deleteOlderThanKeep(userId, 3)).thenReturn(0);

        userSessionService.createSession(user, familyId, "127.0.0.1", "JUnit", Instant.now().plusSeconds(60));

        verify(refreshTokenRepository, never()).deleteByFamilyIdIn(any());
        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getFamilyId()).isEqualTo(familyId);
    }
}
