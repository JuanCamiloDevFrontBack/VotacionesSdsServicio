package com.example.votacionessds.modules.auth.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.votacionessds.modules.auth.dao.LoginAuditRepository;
import com.example.votacionessds.modules.auth.entity.LoginAudit;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.services.LoginAuditService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginAuditServiceImpl implements LoginAuditService {

    private final LoginAuditRepository loginAuditRepository;

    @Override
    @Transactional
    public void recordSuccess(User user, String ip, String userAgent) {
        loginAuditRepository.save(LoginAudit.builder()
                .user(user)
                .emailAttempted(user.getEmail())
                .success(true)
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());
    }

    @Override
    @Transactional // Depronto se tenga que utilizar: (propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String emailAttempted, String ip, String userAgent) {
        loginAuditRepository.save(LoginAudit.builder()
                .emailAttempted(emailAttempted)
                .success(false)
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());
    }
}
