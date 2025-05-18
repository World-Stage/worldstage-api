package com.jonathanfletcher.worldstage_api.service;

import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import com.jonathanfletcher.worldstage_api.model.entity.Stream;
import com.jonathanfletcher.worldstage_api.repository.StreamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamService {

    private final StreamRepository streamRepository;

    public void publishStream(Map<String, String> queryParams) {
        String streamKey = queryParams.get("name");
        log.info("Stream published: {}", streamKey);
        String rtmpUrl = "rtmp://nginx-rtmp:1935/live/" + streamKey;
        String hlsUrl = "http://nginx-rtmp:8080/hls/" + streamKey + ".m3u8";
        Stream stream = new Stream(streamKey, rtmpUrl, hlsUrl, true, StreamStatus.ACTIVE);
        streamRepository.save(stream);
    }
}
