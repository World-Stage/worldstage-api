package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import com.jonathanfletcher.worldstage_api.service.StreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class StreamController {

    private final StreamService streamService;

    @PostMapping("/publish")
    public ResponseEntity<StreamResponse> publishStream(@RequestParam Map<String, String> queryParams) {
        log.info("Establishing a new Stream connection");
        log.info("Query Params {}", queryParams);

        return ResponseEntity.ok(streamService.publishStream(queryParams));
    }

    @PostMapping("/unpublish")
    public ResponseEntity<Void> unPublishStream(@RequestParam Map<String, String> queryParams) {
        log.info("Removing a Stream connection");
        log.info("Query Params {}", queryParams);

        streamService.unPublishStream(queryParams);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/view/active")
    public ResponseEntity<StreamResponse> getActiveStream() {
        log.info("Fetching active stream");

        return ResponseEntity.ok(streamService.getActiveStream());
    }

    @GetMapping("/{streamId}")
    public ResponseEntity<StreamResponse> getStream(@PathVariable UUID streamId) {
        log.info("Fetching stream {}", streamId);
        return ResponseEntity.ok(streamService.getStream(streamId));
    }
}