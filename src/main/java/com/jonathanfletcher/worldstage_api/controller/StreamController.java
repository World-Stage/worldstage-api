package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import com.jonathanfletcher.worldstage_api.service.StreamService;
import com.jonathanfletcher.worldstage_api.service.WebSocketViewerTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final WebSocketViewerTracker webSocketViewerTracker;

    @Value("${spring.security.client.nginx.secret}")
    private String SECRET;

    @PostMapping("/publish")
    public ResponseEntity<StreamResponse> publishStream(@RequestParam UUID name, @RequestParam(required = false) String secret) {
        log.info("Establishing a new Stream connection");
//        if (!secret.equals(SECRET)) {
//            log.warn("Publish Stream failed nginx secret check");
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }

        return ResponseEntity.ok(streamService.publishStream(name));
    }

    @PostMapping("/unpublish")
    public ResponseEntity<Void> unPublishStream(@RequestParam UUID name, @RequestParam(required = false) String secret) {
        log.info("Removing a Stream connection");

//        if (!secret.equals(SECRET)) {
//            log.warn("Cannot unpublish stream as nginx secret failed check");
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }

        streamService.unPublishStream(name);
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

    @GetMapping("/view/count")
    public ResponseEntity<Map<String, Integer>> getViewerCount() {
        return ResponseEntity.ok(Map.of("viewerCount", webSocketViewerTracker.getViewerCount()));
    }
}