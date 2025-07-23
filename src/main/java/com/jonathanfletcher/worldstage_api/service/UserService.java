package com.jonathanfletcher.worldstage_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathanfletcher.worldstage_api.controller.StreamSseController;
import com.jonathanfletcher.worldstage_api.exception.EntityConflictException;
import com.jonathanfletcher.worldstage_api.exception.EntityNotFoundException;
import com.jonathanfletcher.worldstage_api.model.entity.StreamMetadata;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.model.request.StreamMetadataRequest;
import com.jonathanfletcher.worldstage_api.model.request.UserCreateRequest;
import com.jonathanfletcher.worldstage_api.model.response.StreamMetadataResponse;
import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import com.jonathanfletcher.worldstage_api.repository.StreamMetadataRepository;
import com.jonathanfletcher.worldstage_api.repository.StreamRepository;
import com.jonathanfletcher.worldstage_api.repository.UserRepository;
import com.jonathanfletcher.worldstage_api.spring.security.model.ERole;
import com.jonathanfletcher.worldstage_api.spring.security.model.entity.Role;
import com.jonathanfletcher.worldstage_api.spring.security.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final StreamMetadataRepository streamMetadataRepository;

    private final StreamRepository streamRepository;

    private final ObjectMapper objectMapper;

    private final StreamService streamService;

    private final StreamSseController streamSseController;

    public User registerUser(UserCreateRequest request) {
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
                    .streamKey(UUID.randomUUID())
                    .roles(Set.of(roleRepository.findByName(ERole.USER).orElseGet(() -> {
                        log.info("Creating new role: {}", ERole.USER);
                        return roleRepository.save(Role.builder()
                                .id(UUID.randomUUID())
                                .name(ERole.USER)
                                .build());
                    })))
                    .build();
            StreamMetadata newStreamMetadata = StreamMetadata.builder()
                .title(String.format("%s's stream", newUser.getUsername()))
                .userId(newUser.getId())
                .build();
            streamMetadataRepository.save(newStreamMetadata);
            return userRepository.save(newUser);
    }

    public UserResponse getUser(UUID userId, Boolean isUserPrincipal) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        UserResponse response = objectMapper.convertValue(user, UserResponse.class);
        if (isUserPrincipal) {
            streamRepository.findByStreamKeyAndActiveTrue(user.getStreamKey())
                    .ifPresent(stream -> response.setActiveStream(objectMapper.convertValue(stream, StreamResponse.class)));

            streamMetadataRepository.findById(user.getId())
                    .ifPresentOrElse(metadata -> {
                        response.setStreamMetadata(objectMapper.convertValue(metadata, StreamMetadataResponse.class));
                    }, () -> {
                        log.warn("No Stream Metadata set for user {}", user.getId());
                        StreamMetadata metadata = StreamMetadata.builder()
                                .title(String.format("%s's stream", user.getUsername()))
                                .userId(user.getId())
                                .build();
                        streamMetadataRepository.save(metadata);
                        response.setStreamMetadata(objectMapper.convertValue(metadata, StreamMetadataResponse.class));
                    });
        }
        return response;
    }

    public StreamMetadataResponse updateStreamMetadata(StreamMetadataRequest request, UUID userId) {
        StreamMetadata streamMetadata = streamMetadataRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Stream Metadata not found!"));

        Optional.ofNullable(request.getTitle()).ifPresent(streamMetadata::setTitle);
        Optional.ofNullable(request.getDescription()).ifPresent(streamMetadata::setDescription);
        StreamMetadata _streamMetadata = streamMetadataRepository.save(streamMetadata);

        StreamMetadataResponse streamMetadataResponse = objectMapper.convertValue(_streamMetadata, StreamMetadataResponse.class);

        streamRepository.findByUserIdAndActiveTrue(userId)
            .flatMap(stream -> streamRepository.findByActiveTrue()
                    .filter(activeStream -> stream.getId().equals(activeStream.getId()))
            )
            .ifPresent(activeStream -> {
                log.info("Metadata Update is for active stream");
                activeStream.setTitle(_streamMetadata.getTitle());
                activeStream.setDescription(_streamMetadata.getDescription());
                streamRepository.save(activeStream);
                streamSseController.notifyActiveStreamMetadataChange(streamMetadataResponse);
            });

        return streamMetadataResponse;
    }

    public UserResponse regenerateStreamKey(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User does not exist"));
        streamRepository.findByUserIdAndActiveTrue(user.getId()).ifPresent(stream -> {
            log.info("User {} is regenerating stream key when live (stream {}, ending current stream", userId, stream.getId());
            streamService.unPublishStream(stream.getStreamKey());
        });

        user.setStreamKey(UUID.randomUUID());
        User _user = userRepository.save(user);
        log.info("Update stream key for user {}", user.getId());

        return objectMapper.convertValue(_user, UserResponse.class);
    }
}
