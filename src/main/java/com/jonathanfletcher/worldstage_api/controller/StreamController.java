package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.model.entity.Stream;
import com.jonathanfletcher.worldstage_api.repository.StreamRepository;
//import com.jonathanfletcher.worldstage_api.service.StreamService;
import com.jonathanfletcher.worldstage_api.service.StreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/stream")
@RequiredArgsConstructor
public class StreamController {

    private final StreamService streamService;

    @PostMapping("/publish")
    public void publishStream(@RequestParam Map<String, String> queryParams) {
        log.info("Query Params {}", queryParams);

//        User user = userRepository.findByStreamKey(streamKey)
//                .orElseThrow(() -> new RuntimeException("Invalid stream key"));
//        // Check if stream already exists in queue
//        if (streamRepository.findByStreamKeyAndStatusNot(streamKey, "ended").isEmpty()) {
//            String rtmpUrl = "rtmp://nginx-rtmp:1935/live/" + streamKey;
//            String hlsUrl = "http://nginx-rtmp:8080/hls/" + streamKey + ".m3u8";
//            Stream stream = new Stream(streamKey, rtmpUrl, hlsUrl, true);
//            streamRepository.save(stream);
//        }
    }

    @PostMapping("unpublish")
    public void unPublishStream(@RequestParam Map<String, String> queryParams) {
        log.info("Query Params {}", queryParams);
        String streamKey = queryParams.get("name");
        log.info("Stream published: {}", streamKey);
        if (streamKey == null) {
            log.error("No valid stream key provided");
            throw new RuntimeException("No valid stream key");
        }

        streamRepository.findByStreamKeyAndStatusNot(streamKey, "ended")
                .ifPresent(stream -> {
                    stream.setActive(false);
                    stream.setStatus("ended");
                    streamRepository.save(stream);
                    log.info("Stream {} marked as ended", key);
                });
    }

    @GetMapping("/view/active")
    public Stream getActiveStream() {
        log.info("Fetching active stream");
        return streamRepository.findByActiveTrue()
                .orElseThrow(() -> new RuntimeException("No active stream"));
    }

    @GetMapping("/view")
    public Stream getStream(@RequestParam String streamKey) {
        log.info("Fetching stream with key {}", streamKey);
        return streamRepository.findById(streamKey)
                .orElseThrow(() -> new RuntimeException("Stream not found"));
    }
}