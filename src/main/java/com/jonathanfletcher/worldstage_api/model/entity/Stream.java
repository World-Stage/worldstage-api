package com.jonathanfletcher.worldstage_api.model.entity;

import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@Entity
@Table(name = "streams")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Stream {

    @Id
    private UUID id;

    private UUID streamKey;

    private String rtmpUrl;

    private String hlsUrl;

    private Boolean active;

    @Enumerated(EnumType.STRING)
    private StreamStatus status;
}
