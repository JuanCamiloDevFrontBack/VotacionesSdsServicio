package com.example.votacionessds.modules.auth.dao;

import com.example.votacionessds.modules.auth.entity.LoginAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginAuditRepository extends JpaRepository<LoginAudit, Long> {
}
