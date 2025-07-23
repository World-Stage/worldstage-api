package com.jonathanfletcher.worldstage_api.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({
        "id",
        "streamKey",
        "streamer",
        "rtmpUrl",
        "hlsUrl",
        "active",
        "title",
        "description",
        "status",
})
public class StreamResponse {

    private UUID id;

    private UUID streamKey;

    private String rtmpUrl;

    private String hlsUrl;

    private Boolean active;

    private String title;

    private String description;

    private StreamStatus status;

    private UserResponse user;
}
