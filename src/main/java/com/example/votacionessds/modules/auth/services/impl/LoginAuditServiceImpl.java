package com.example.votacionessds.modules.auth.services.impl;

<<<<<<< Updated upstream
=======
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
>>>>>>> Stashed changes
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.votacionessds.modules.auth.dao.LoginAuditRepository;
<<<<<<< Updated upstream
=======
import com.example.votacionessds.modules.auth.dao.UserRepository;
>>>>>>> Stashed changes
import com.example.votacionessds.modules.auth.entity.LoginAudit;
import com.example.votacionessds.modules.auth.entity.User;
import com.example.votacionessds.modules.auth.services.LoginAuditService;

import lombok.RequiredArgsConstructor;
<<<<<<< Updated upstream

=======
import lombok.extern.slf4j.Slf4j;

@Slf4j
>>>>>>> Stashed changes
@Service
@RequiredArgsConstructor
public class LoginAuditServiceImpl implements LoginAuditService {

    private final LoginAuditRepository loginAuditRepository;
<<<<<<< Updated upstream
=======
    private final UserRepository userRepository;

    @Value("${security.retention.max-login-audits:3}")
    private int maxLoginAudits;
>>>>>>> Stashed changes

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
<<<<<<< Updated upstream
    }

    @Override
    @Transactional // Depronto se tenga que utilizar: (propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String emailAttempted, String ip, String userAgent) {
        loginAuditRepository.save(LoginAudit.builder()
=======
        pruneByUserId(user.getId());
    }

    @Override
    @Transactional
    public void recordFailure(String emailAttempted, String ip, String userAgent) {
        User user = userRepository.findByEmail(emailAttempted).orElse(null);
        loginAuditRepository.save(LoginAudit.builder()
                .user(user)
>>>>>>> Stashed changes
                .emailAttempted(emailAttempted)
                .success(false)
                .ipAddress(ip)
                .userAgent(userAgent)
                .build());
<<<<<<< Updated upstream
=======
        if (user != null) {
            pruneByUserId(user.getId());
        } else {
            pruneByEmailWithoutUser(emailAttempted);
        }
    }

    private void pruneByUserId(UUID userId) {
        if (maxLoginAudits <= 0) {
            return;
        }
        int deleted = loginAuditRepository.deleteOlderThanKeepByUserId(userId, maxLoginAudits);
        if (deleted > 0) {
            log.info("Pruned older login audits by user: userId={}, deleted={}", userId, deleted);
        }
    }

    private void pruneByEmailWithoutUser(String email) {
        if (maxLoginAudits <= 0) {
            return;
        }
        int deleted = loginAuditRepository.deleteOlderThanKeepByEmailWithoutUser(email, maxLoginAudits);
        if (deleted > 0) {
            log.info("Pruned older anonymous login audits: email={}, deleted={}", email, deleted);
        }
>>>>>>> Stashed changes
    }
}
