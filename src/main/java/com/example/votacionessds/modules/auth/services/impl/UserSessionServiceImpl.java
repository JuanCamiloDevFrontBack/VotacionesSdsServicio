package com.example.votacionessds.modules.auth.services.impl;

import com.example.votacionessds.modules.auth.dao.UserRepository;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.entity.UserSession;
import com.example.votacionessds.modules.auth.services.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private final UserRepository userRepository;

    @Override
    public UserSession createSession(User user, String ip, String userAgent) {
        UserSession s = UserSession.builder()
                .user(user)
                .ipAddress(ip)
                .userAgent(userAgent)
                .loginAt(Instant.now())
                .active(true)
                .build();
        // Persist via userRepository's entity manager cascade is not set; use save through repository if exists.
        // For simplicity create via userRepository.save(user) not appropriate; better to use JPA repository for sessions.
        return s;
    }

    @Override
    public void invalidateSession(Long sessionId) {
        // Implementation placeholder: actual session repo should be used. Keep method for API surface.
    }
}
