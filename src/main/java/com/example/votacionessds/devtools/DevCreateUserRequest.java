package com.example.votacionessds.devtools;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO exclusivo del paquete devtools. Nota lo que NO tiene: campo roleId.
 * A propósito — el rol nunca lo elige el caller (ver DevUserTestController).
 */
public record DevCreateUserRequest(

        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 128)
        String password
) {}
