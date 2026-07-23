package com.example.votacionessds.modules.auth.services.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.votacionessds.modules.auth.dao.LoginAuditRepository;
import com.example.votacionessds.modules.auth.dao.UserRepository;
import com.example.votacionessds.modules.auth.entity.LoginAudit;
import com.example.votacionessds.modules.auth.entity.User;

@ExtendWith(MockitoExtension.class)
class LoginAuditServiceImplRetentionTest {

    @Mock
    private LoginAuditRepository loginAuditRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoginAuditServiceImpl loginAuditService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginAuditService, "maxLoginAudits", 3);
        user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .username("user")
                .build();
    }

    @Test
    void recordSuccess_prunesByUserId() {
        when(loginAuditRepository.save(any(LoginAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginAuditRepository.deleteOlderThanKeepByUserId(user.getId(), 3)).thenReturn(2);

        loginAuditService.recordSuccess(user, "127.0.0.1", "JUnit");

        verify(loginAuditRepository).deleteOlderThanKeepByUserId(eq(user.getId()), eq(3));
        verify(loginAuditRepository, never()).deleteOlderThanKeepByEmailWithoutUser(any(), any(Integer.class));
    }

    @Test
    void recordFailure_withKnownUser_prunesByUserId() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(loginAuditRepository.save(any(LoginAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginAuditRepository.deleteOlderThanKeepByUserId(user.getId(), 3)).thenReturn(1);

        loginAuditService.recordFailure("user@example.com", "127.0.0.1", "JUnit");

        verify(loginAuditRepository).deleteOlderThanKeepByUserId(user.getId(), 3);
        verify(loginAuditRepository, never()).deleteOlderThanKeepByEmailWithoutUser(any(), any(Integer.class));
    }

    @Test
    void recordFailure_withUnknownEmail_prunesByEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(loginAuditRepository.save(any(LoginAudit.class))).thenAnswer(inv -> inv.getArgument(0));
        when(loginAuditRepository.deleteOlderThanKeepByEmailWithoutUser("missing@example.com", 3)).thenReturn(1);

        loginAuditService.recordFailure("missing@example.com", "127.0.0.1", "JUnit");

        verify(loginAuditRepository).deleteOlderThanKeepByEmailWithoutUser("missing@example.com", 3);
        verify(loginAuditRepository, never()).deleteOlderThanKeepByUserId(any(), any(Integer.class));
    }
}
