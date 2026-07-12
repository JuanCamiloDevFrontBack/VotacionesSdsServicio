package com.example.votacionessds.modules.auth.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUsernameByRefreshTokenTestRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String username;

    @NotBlank
    private String refreshToken;
}
