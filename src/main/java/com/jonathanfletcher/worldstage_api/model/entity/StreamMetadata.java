package com.jonathanfletcher.worldstage_api.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Builder
@Entity
@Table(name = "streams_metadata")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class StreamMetadata {

    @Id
    private UUID userId;

    @NonNull
    @Size(max = 100)
    private String title;

    @Size(max = 1000)
    private String description;

    @CreationTimestamp
    private Instant createdTs;

    @UpdateTimestamp
    private Instant lastModifiedTs;
}
