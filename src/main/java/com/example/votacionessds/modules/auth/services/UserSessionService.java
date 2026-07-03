package com.example.votacionessds.modules.auth.services;

import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.entity.UserSession;

public interface UserSessionService {
    UserSession createSession(User user, String ip, String userAgent);
    void invalidateSession(Long sessionId);
}
