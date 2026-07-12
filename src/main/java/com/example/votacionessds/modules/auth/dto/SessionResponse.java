package com.example.votacionessds.modules.auth.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private UUID id;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
    private Instant lastSeenAt;
    private Instant expiresAt;
    private boolean current;
}
