package com.jonathanfletcher.worldstage_api.proxy.mock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@Profile({ "test" })
@RequestMapping(path = "/mock/transcoder")
@RequiredArgsConstructor
public class MockTranscoderController {

    @PostMapping(path = "/transcode/{streamKey}")
    public ResponseEntity<Void> createEvent(@PathVariable UUID streamKey) {
        log.info("Received mock request to start transcoding stream {}", streamKey);

        return ResponseEntity.ok().build();
    }
}
