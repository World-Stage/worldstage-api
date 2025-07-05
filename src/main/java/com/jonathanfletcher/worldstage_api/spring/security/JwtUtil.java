package com.jonathanfletcher.worldstage_api.spring.security;

import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.spring.security.model.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtUtil {
    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final long accessTokenExpirationMs = 1000L * 60 * 15; // 15 minutes
    private final long refreshTokenExpirationMs = 1000L * 60 * 60 * 24 * 7; // 7 days

    public JwtUtil(
            @Value("${spring.security.jwt.access-secret}") String accessSecret,
            @Value("${spring.security.jwt.refresh-secret}") String refreshSecret) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(String username, UUID familyId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("familyId", familyId.toString());
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(refreshKey)
                .compact();
    }

    public void setRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
        log.debug("Set refresh token cookie");
    }

    public String getUsernameFromToken(String token, boolean isRefreshToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(isRefreshToken ? refreshKey : accessKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            log.warn("Invalid token: {}", e.getMessage());
            return null;
        }
    }

    public UUID getFamilyIdFromRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(refreshKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String familyId = claims.get("familyId", String.class);
            return UUID.fromString(familyId);
        } catch (Exception e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return null;
        }
    }

    public Set<String> getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(accessKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            @SuppressWarnings("unchecked")
            List<String> rolesList = claims.get("roles", List.class);

            return new HashSet<>(rolesList);
        } catch (Exception e) {
            log.warn("Invalid access token: {}", e.getMessage());
            return Set.of();
        }
    }

    @Profile("test")
    public String generateExpiredRefresh(String username, UUID familyId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("familyId", familyId.toString());
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() - refreshTokenExpirationMs))
                .signWith(refreshKey)
                .compact();
    }
    public boolean validateToken(String token, boolean isRefreshToken) {
        try {
            Jwts.parser()
                    .verifyWith(isRefreshToken ? refreshKey : accessKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
