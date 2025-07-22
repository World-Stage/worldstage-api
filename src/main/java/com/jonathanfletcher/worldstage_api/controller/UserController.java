package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import com.jonathanfletcher.worldstage_api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping(path = "/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId, @RequestParam( defaultValue = "false") Boolean returnActiveStream) {
        log.info("Request received to get user {} with returnActiveStream {}", userId, returnActiveStream);

        return ResponseEntity.ok(userService.getUser(userId, returnActiveStream));
    }
}
