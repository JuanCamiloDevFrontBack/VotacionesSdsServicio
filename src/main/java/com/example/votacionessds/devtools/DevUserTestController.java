package com.example.votacionessds.devtools;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.votacionessds.exceptions.ConflictException;
import com.example.votacionessds.exceptions.ErrorCode;
import com.example.votacionessds.modules.auth.dao.RoleRepository;
import com.example.votacionessds.modules.auth.dao.UserRepository;
import com.example.votacionessds.modules.auth.entity.Role;
import com.example.votacionessds.modules.auth.entity.User;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ⚠️ SOLO PARA DESARROLLO — paquete completo pensado para borrarse entero
 * cuando ya no haga falta (una sola carpeta, cero dependencias del resto
 * de la app hacia acá).
 *
 * @Profile("!prod"): estos endpoints NO EXISTEN como beans si el perfil
 * activo es "prod" — no depende de que SecurityConfig los bloquee bien,
 * Spring ni siquiera los registra.
 *
 * El endpoint de creación es intencionalmente público (sin JWT), a pedido
 * explícito, PERO:
 *   - nunca asigna el rol que el cliente pida (no existe ese campo en el DTO)
 *   - siempre crea con un rol fijo y de bajo privilegio (ROLE_VOTE)
 * Esto evita reproducir la escalación de privilegios que tenía el
 * /api/users original (roleId libre + endpoint público).
 */
@Slf4j
@Profile("!prod")
@RestController
@RequestMapping("/api/dev/users")
@RequiredArgsConstructor
public class DevUserTestController {

    private static final String DEFAULT_TEST_ROLE = "ROLE_VOTE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    void warnActive() {
        log.warn("############################################################");
        log.warn("# DevUserTestController ACTIVO — endpoints de prueba sin auth #");
        log.warn("# para crear usuarios. NO debe estar vivo en producción.      #");
        log.warn("############################################################");
    }

    @PostMapping
    public ResponseEntity<DevUserSummary> createTestUser(@Validated @RequestBody DevCreateUserRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new ConflictException(ErrorCode.USERNAME_EXISTS, "Username already exists");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException(ErrorCode.EMAIL_EXISTS, "Email already exists");
        }

        Role defaultRole = roleRepository.findByName(DEFAULT_TEST_ROLE)
                .orElseThrow(() -> new IllegalStateException(
                        "Rol '" + DEFAULT_TEST_ROLE + "' no existe en BD — créalo antes de usar este endpoint"));

        Instant now = Instant.now();
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .enabled(true)
                .emailVerified(false)
                .accountLocked(false)
                .failedLoginAttempts((short) 0)
                .passwordChangedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .roles(Set.of(defaultRole))
                .build();

        User saved = userRepository.save(user);
        log.warn("[DEV] Usuario de prueba creado sin autenticación: id={}, username={}",
                saved.getId(), saved.getUsername());

        return ResponseEntity.status(HttpStatus.CREATED).body(toSummary(saved));
    }

    /** Requiere un access token válido (cualquier usuario autenticado, sin chequeo de rol). */
    @GetMapping
    public ResponseEntity<List<DevUserSummary>> listUsers() {
        List<DevUserSummary> users = userRepository.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    private DevUserSummary toSummary(User user) {
        return DevUserSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .enabled(user.isEnabled())
                .accountLocked(user.isAccountLocked())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
