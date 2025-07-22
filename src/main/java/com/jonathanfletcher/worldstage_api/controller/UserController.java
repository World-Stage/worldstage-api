package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.model.IsUser;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.model.request.StreamMetadataRequest;
import com.jonathanfletcher.worldstage_api.model.response.StreamMetadataResponse;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import com.jonathanfletcher.worldstage_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping(path = "/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId, @AuthenticationPrincipal User userDetails) {
        boolean isUserPrincipal = userId.equals(userDetails.getId());
        log.info("Request received to get user {} with isUserPrincipal {}", userId, isUserPrincipal);

        return ResponseEntity.ok(userService.getUser(userId, isUserPrincipal));
    }

    @IsUser
    @PatchMapping(path = "/{userId}/streamMetadata")
    public ResponseEntity<StreamMetadataResponse> updateStreamMetadata(@PathVariable UUID userId,
                                                                       @Valid @RequestBody StreamMetadataRequest request) {
        log.info("Updating user {} stream metadata", userId);

        return ResponseEntity.ok(userService.updateStreamMetadata(request, userId));
    }
}
