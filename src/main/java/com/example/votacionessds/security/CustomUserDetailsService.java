package com.example.votacionessds.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.votacionessds.modules.auth.dao.UserRepository;
import com.example.votacionessds.modules.auth.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                // Mensaje genérico: no revelar si el email existe (evita user enumeration).
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Set<GrantedAuthority> authorities = (user.getRoles() == null || user.getRoles().isEmpty())
                ? Set.of(new SimpleGrantedAuthority("ROLE_VOTE"))
                : user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .accountLocked(!user.isLoginAllowed())
                .build();
    }
}
