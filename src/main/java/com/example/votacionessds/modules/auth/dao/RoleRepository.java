package com.example.votacionessds.modules.auth.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.votacionessds.modules.auth.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByName(String name);
}
