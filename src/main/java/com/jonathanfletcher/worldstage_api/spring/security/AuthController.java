package com.jonathanfletcher.worldstage_api.spring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.model.request.AuthRequest;
import com.jonathanfletcher.worldstage_api.model.request.UserCreateRequest;
import com.jonathanfletcher.worldstage_api.model.response.AuthResponse;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import com.jonathanfletcher.worldstage_api.repository.UserRepository;
import com.jonathanfletcher.worldstage_api.service.UserService;
import com.jonathanfletcher.worldstage_api.spring.security.model.entity.RefreshToken;
import com.jonathanfletcher.worldstage_api.spring.security.service.TokenService;
import com.jonathanfletcher.worldstage_api.spring.security.service.UserDetailsServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(path = "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    private final JwtUtil jwtUtil;

    private final UserService userService;

    private final TokenService tokenService;

    private final UserRepository userRepository;

    private final UserDetailsServiceImpl userDetailsService;

    private final ObjectMapper objectMapper;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody UserCreateRequest registerRequest, HttpServletResponse response) {
        User registeredUser = userService.registerUser(registerRequest);

        log.info("User registered: {}", registerRequest.getUsername());
        AuthResponse responseBody = authenticateUser(registeredUser, response);

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest, HttpServletResponse response) {
        Optional<User> emailedUser = userRepository.findByEmail(authRequest.getUsername()); //Check if user used email
        String userAuth = emailedUser.map(value -> value.getUsername().toLowerCase())
                .orElseGet(() -> authRequest.getUsername().toLowerCase());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                userAuth, authRequest.getPassword()));


        User user = (User) userDetailsService.loadUserByUsername(userAuth);
        AuthResponse responseBody = authenticateUser(user, response);
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken != null) {
            UUID familyId = jwtUtil.getFamilyIdFromRefreshToken(refreshToken);
            if (familyId != null && jwtUtil.validateToken(refreshToken, true)) {
                String username = jwtUtil.getUsernameFromToken(refreshToken, true);
                Optional<RefreshToken> tokenOpt = tokenService.validateRefreshToken(refreshToken, familyId);
                if (tokenOpt.isPresent()) {
                    User user = (User) userDetailsService.loadUserByUsername(username);
                    String newAccessToken = jwtUtil.generateAccessToken(user);
                    String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername(), familyId);
                    tokenService.storeRefreshToken(newRefreshToken, user.getUsername(), familyId);
                    jwtUtil.setRefreshTokenCookie(newRefreshToken, response);
                    log.info("Refreshed access token for user: {}", user.getUsername());
                    return ResponseEntity.ok(AuthResponse.builder()
                            .accessToken(newAccessToken)
                            .user(objectMapper.convertValue(user, UserResponse.class))
                            .build()
                    );
                }
            }
        }
        log.warn("Invalid refresh token attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken != null) {
            UUID familyId = jwtUtil.getFamilyIdFromRefreshToken(refreshToken);
            String username = jwtUtil.getUsernameFromToken(refreshToken, true);
            if (familyId != null && username != null) {
                tokenService.invalidateSession(username, familyId);
                log.info("User logged out: {}", username);
            }
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getUserFromPrincipal(@AuthenticationPrincipal User userDetails) {
        return ResponseEntity.ok(UserResponse.builder()
                        .id(userDetails.getId())
                        .email(userDetails.getEmail())
                        .username(userDetails.getUsername())
                        .createdTs(userDetails.getCreatedTs())
                        .lastModifiedTs(userDetails.getLastModifiedTs())
                .build());
    }

    private AuthResponse authenticateUser(User user, HttpServletResponse response) {
        String accessToken = jwtUtil.generateAccessToken(user);
        UUID familyId = UUID.randomUUID();
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername(), familyId);
        tokenService.storeRefreshToken(refreshToken, user.getUsername(), familyId);
        jwtUtil.setRefreshTokenCookie(refreshToken, response);
        log.info("User logged in: {}", user.getUsername());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .user(objectMapper.convertValue(user, UserResponse.class))
                .build();
    }

//    @GetMapping("/csrf")
//    public ResponseEntity<?> getCsrfToken(CsrfToken csrfToken) {
//        log.info("CSRF token requested");
//        return ResponseEntity.ok()
//                .header("X-CSRF-TOKEN", csrfToken.getToken())
//                .build();
//    }
}
