package com.jonathanfletcher.worldstage_api.spring.security.model.entity;

import com.jonathanfletcher.worldstage_api.model.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Builder
@Entity
@Table(name = "refresh_token", schema = "edge")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private UUID familyId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Instant expiresAt;

    @CreationTimestamp
    private Instant createdTs;

    @UpdateTimestamp
    private Instant lastModifiedTs;
}
