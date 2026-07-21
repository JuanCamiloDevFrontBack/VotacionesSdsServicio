package com.example.votacionessds.modules.auth.restcontroller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    //private final AuthService authService;

    /*@PostMapping
    public ResponseEntity<UserResponse> createUser(@Validated @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUser(request));
    }*/
}
