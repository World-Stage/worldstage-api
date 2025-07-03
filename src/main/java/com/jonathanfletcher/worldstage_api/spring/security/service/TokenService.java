package com.jonathanfletcher.worldstage_api.spring.security.service;

import com.jonathanfletcher.worldstage_api.exception.EntityNotFoundException;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.repository.UserRepository;
import com.jonathanfletcher.worldstage_api.spring.security.model.entity.RefreshToken;
import com.jonathanfletcher.worldstage_api.spring.security.repository.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long maxSessionLifetimeMs = 1000L * 60 * 60 * 24 * 30; // 30 days
    private final long refreshTokenExpirationMs = 1000L * 60 * 60 * 24 * 7; // 7 days

    @Transactional
    public RefreshToken storeRefreshToken(String refreshToken, String username, UUID familyId) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User was not found for refresh token"));
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(refreshTokenExpirationMs);

        // Hash the refresh token before storing
        String tokenHash = hashToken(refreshToken);

        // Delete existing token for the same user and family
        refreshTokenRepository.deleteByUserAndFamilyId(user, familyId);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .tokenHash(tokenHash)
                .familyId(familyId)
                .expiresAt(expiresAt)
                .user(user)
                .build();
                log.info("Stored refresh token for user: {}", username);
        return refreshTokenRepository.save(refreshTokenEntity);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> validateRefreshToken(String refreshToken, UUID familyId) {
        String tokenHash = hashToken(refreshToken);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHashAndFamilyId(tokenHash, familyId);
        if (tokenOpt.isPresent()) {
            RefreshToken token = tokenOpt.get();
            Instant now = Instant.now();
            if (token.getExpiresAt().isAfter(now) &&
                    token.getCreatedTs().plusMillis(maxSessionLifetimeMs).isAfter(now)) {
                log.debug("Validated refresh token for familyId: {}", familyId);
                return Optional.of(token);
            } else {
                log.warn("Refresh token expired or session too old for familyId: {}", familyId);
                refreshTokenRepository.deleteById(token.getId());
            }
        }
        log.warn("Invalid refresh token for familyId: {}", familyId);
        return Optional.empty();
    }

    @Transactional
    public void invalidateSession(String username, UUID familyId) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User was not found for refresh token"));
        refreshTokenRepository.deleteByUserAndFamilyId(user, familyId);
        log.info("Invalidated session for user: {}, familyId: {}", username, familyId);
    }

    @Transactional
    @Scheduled(fixedRate = 86400000) // Run daily
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(Instant.now());
        log.info("Cleared expired refresh tokens");
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashedBytes) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 not available, falling back to default hash", e);
            return Integer.toString(token.hashCode()); // Fallback
        }
    }
}
