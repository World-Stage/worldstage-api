package com.jonathanfletcher.worldstage_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.model.request.UserCreateRequest;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import com.jonathanfletcher.worldstage_api.repository.UserRepository;
import com.jonathanfletcher.worldstage_api.spring.security.model.ERole;
import com.jonathanfletcher.worldstage_api.spring.security.model.entity.Role;
import com.jonathanfletcher.worldstage_api.spring.security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;

public UserResponse registerUser(UserCreateRequest request) {
        User newUser = User.builder()
                .id(UUID.randomUUID())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(roleRepository.findByName(ERole.ROLE_USER.toString()).orElseGet(() -> {
                    log.info("Creating new role: {}", ERole.ROLE_USER);
                    return roleRepository.save(Role.builder()
                            .id(UUID.randomUUID())
                            .name(ERole.ROLE_USER)
                            .build());
                })))
                .build();

        User _user = userRepository.save(newUser);

        return objectMapper.convertValue(_user, UserResponse.class);
    }
}
