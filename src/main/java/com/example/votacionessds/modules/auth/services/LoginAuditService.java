package com.example.votacionessds.modules.auth.services;

import com.example.votacionessds.modules.auth.entity.User;

public interface LoginAuditService {
    void recordSuccess(User user, String ip, String userAgent);

    void recordFailure(String emailAttempted, String ip, String userAgent);
}
