package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.exception.EntityNotFoundException;
import com.jonathanfletcher.worldstage_api.model.entity.Stream;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import com.jonathanfletcher.worldstage_api.repository.StreamRepository;
import com.jonathanfletcher.worldstage_api.service.StreamService;
import com.jonathanfletcher.worldstage_api.service.WebSocketViewerTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/streams")
@RequiredArgsConstructor
public class StreamController {

    private final StreamService streamService;

    private final WebSocketViewerTracker webSocketViewerTracker;

    private final StreamRepository streamRepository;

    @Value("${spring.security.client.nginx.secret}")
    private String SECRET;

    /*
        Gets called by nginx server.
    */
    @PostMapping("/publish")
    public ResponseEntity<StreamResponse> publishStream(@RequestParam UUID name, @RequestParam(required = false) String secret) {
        log.info("Establishing a new Stream connection");
//        if (!secret.equals(SECRET)) {
//            log.warn("Publish Stream failed nginx secret check");
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }

        return ResponseEntity.ok(streamService.publishStream(name));
    }

    /*
        Gets called by nginx server.
     */
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

    /*
       Gets called from stream manager front end
     */
    @DeleteMapping("/{streamId}/unpublish")
    public ResponseEntity<Void> unpublishSpecificStream(@PathVariable UUID streamId, @AuthenticationPrincipal User userDetails) {
        log.info("User {} is attempting to stop stream {}", userDetails.getId(), streamId);

        Stream stream = streamRepository.findById(streamId).orElseThrow(() -> new EntityNotFoundException("Stream does not exist"));

        if ((!stream.getUserId().equals(userDetails.getId()) && userDetails.getAuthorities().stream()
                .noneMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")))) {
            log.warn("User {} is attempting to end stream {} that is owned by other user {}", userDetails.getId(), stream.getId(), stream.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        streamService.unPublishStream(stream.getStreamKey());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/view/count")
    public ResponseEntity<Map<String, Integer>> getViewerCount() {
        return ResponseEntity.ok(Map.of("viewerCount", webSocketViewerTracker.getViewerCount()));
    }
}