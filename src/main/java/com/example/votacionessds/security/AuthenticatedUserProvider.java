package com.example.votacionessds.security;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.example.votacionessds.exceptions.ErrorCode;
import com.example.votacionessds.exceptions.InvalidCredentialsException;
import com.example.votacionessds.exceptions.ResourceNotFoundException;
import com.example.votacionessds.modules.auth.dao.UserRepository;
import com.example.votacionessds.modules.auth.entity.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserProvider {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        String email = getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public String getCurrentUserEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new InvalidCredentialsException(ErrorCode.UNAUTHORIZED, "User is not authenticated");
        }
        return userDetails.getUsername();
    }
}
