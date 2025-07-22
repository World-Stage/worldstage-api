package com.jonathanfletcher.worldstage_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathanfletcher.worldstage_api.exception.EntityConflictException;
import com.jonathanfletcher.worldstage_api.exception.EntityNotFoundException;
import com.jonathanfletcher.worldstage_api.exception.InvalidStreamKeyException;
import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import com.jonathanfletcher.worldstage_api.model.entity.Stream;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import com.jonathanfletcher.worldstage_api.proxy.TranscoderProxy;
import com.jonathanfletcher.worldstage_api.repository.StreamRepository;
import com.jonathanfletcher.worldstage_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamService {

    private final StreamRepository streamRepository;

    private final UserRepository userRepository;

    private final StreamQueueService streamQueueService;

    private final TranscoderProxy transcoderProxy;

    private final ObjectMapper objectMapper;

    public StreamResponse publishStream(UUID streamKey) {
        //TODO: Verify streamkey is correct
        User user = userRepository.findByStreamKey(streamKey).orElseThrow(() -> new InvalidStreamKeyException("The stream key does not exist"));

        streamRepository.findByStreamKeyAndActiveTrue(user.getStreamKey()).ifPresent(stream -> {
            log.warn("");
            throw new EntityConflictException("Stream is already active");
        });

        log.info("Stream published: {}", streamKey);
        String rtmpUrl = "rtmp://nginx-rtmp:1935/live/" + streamKey;
        String hlsUrl = "http://nginx-rtmp:8080/hls/" + streamKey + "/index.m3u8";
        Stream stream = Stream.builder()
                .id(UUID.randomUUID())
                .streamKey(user.getStreamKey())
                .rtmpUrl(rtmpUrl)
                .hlsUrl(hlsUrl)
                .userId(user.getId())
                .active(false)
                .status(StreamStatus.QUEUED)
                .build();

        transcoderProxy.transcodeStream(streamKey);
        Stream _stream = streamRepository.save(stream);
        streamQueueService.addStreamToQueue(_stream);
        return objectMapper.convertValue(_stream, StreamResponse.class);
    }

    public void unPublishStream(UUID streamKey) {
        if (streamKey == null) {
            log.error("No valid stream key provided");
            throw new RuntimeException("No valid stream key");
        }

        streamRepository.findByStreamKeyAndStatusNot(streamKey, StreamStatus.ENDED)
                .ifPresent(stream -> {
                    stream.setActive(false);
                    stream.setStatus(StreamStatus.ENDED);
                    streamRepository.save(stream);
                    log.info("Stream {} marked as ended", stream.getId());
                    streamQueueService.removeStreamFromQueue(stream);
                    transcoderProxy.stopTranscode(streamKey);
                });
    }

    public StreamResponse getActiveStream() {
        return streamRepository.findByActiveTrue()
                .map(stream -> {
                    log.info("Found active stream {}", stream.getId());
                    return objectMapper.convertValue(stream, StreamResponse.class);
                })
                .orElseThrow(() -> new EntityNotFoundException("No active stream"));
    }

    public StreamResponse getStream(UUID streamId) {
        return streamRepository.findById(streamId)
                .map(stream -> objectMapper.convertValue(stream, StreamResponse.class))
                .orElseThrow(() -> new EntityNotFoundException(String.format("Stream %s not found", streamId)));
    }
}
