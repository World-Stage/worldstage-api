package com.jonathanfletcher.worldstage_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathanfletcher.worldstage_api.exception.EntityConflictException;
import com.jonathanfletcher.worldstage_api.exception.EntityNotFoundException;
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
        if (userRepository.existsByUsername(request.getUsername().toLowerCase())) {
            throw new EntityConflictException(String.format("Username %s is already taken!", request.getUsername()));
        }

        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new EntityConflictException(String.format("Email %s is already being used!", request.getEmail()));
        }
            User newUser = User.builder()
                    .id(UUID.randomUUID())
                    .username(request.getUsername().toLowerCase())
                    .email(request.getEmail().toLowerCase())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .roles(Set.of(roleRepository.findByName(ERole.USER).orElseGet(() -> {
                        log.info("Creating new role: {}", ERole.USER);
                        return roleRepository.save(Role.builder()
                                .id(UUID.randomUUID())
                                .name(ERole.USER)
                                .build());
                    })))
                    .build();

            User _user = userRepository.save(newUser);

            return objectMapper.convertValue(_user, UserResponse.class);
    }

    public UserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));

        return objectMapper.convertValue(user, UserResponse.class);
    }
}
