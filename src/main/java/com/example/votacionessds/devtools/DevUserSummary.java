package com.example.votacionessds.devtools;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import lombok.Builder;

@Builder
public record DevUserSummary(
        UUID id,
        String username,
        String email,
        Set<String> roles,
        boolean enabled,
        boolean accountLocked,
        Instant createdAt
) {}
