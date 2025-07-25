package com.jonathanfletcher.worldstage_api.model.entity;

import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Builder
@Entity
@Table(name = "streams", schema = "edge")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Stream {

    @Id
    private UUID id;

    @NonNull
    private UUID streamKey;

    private String rtmpUrl;

    @NonNull
    private String hlsUrl;

    @NonNull
    private UUID userId;

    private Boolean active;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private StreamStatus status;

    @CreationTimestamp
    private Instant createdTs;

    @UpdateTimestamp
    private Instant lastModifiedTs;
}
