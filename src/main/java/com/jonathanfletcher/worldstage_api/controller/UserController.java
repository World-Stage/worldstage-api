package com.jonathanfletcher.worldstage_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.model.request.UserCreateRequest;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import com.jonathanfletcher.worldstage_api.repository.UserRepository;
import com.jonathanfletcher.worldstage_api.service.UserService;
import com.jonathanfletcher.worldstage_api.spring.security.model.ERole;
import com.jonathanfletcher.worldstage_api.spring.security.model.entity.Role;
import com.jonathanfletcher.worldstage_api.spring.security.repository.RoleRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody @Valid UserCreateRequest request) {
        log.info("A new user is trying to sign up with email: {}", request.getEmail());

        return ResponseEntity.ok(userService.registerUser(request));
    }

    @GetMapping(path = "/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        log.info("Request received to get user {}", userId);

        return ResponseEntity.ok(userService.getUser(userId));
    }
}
