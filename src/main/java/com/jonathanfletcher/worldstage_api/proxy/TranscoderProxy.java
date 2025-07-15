package com.jonathanfletcher.worldstage_api.proxy;

import com.jonathanfletcher.worldstage_api.proxy.property.TranscoderProxyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscoderProxy {

    private final RestTemplate restTemplate;

    private final TranscoderProxyProperties properties;

    public void transcodeStream(UUID streamKey) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .pathSegment("transcode")
                .pathSegment(streamKey.toString())
                .build().toUri();

        log.info("Sending request to start transcoding for stream {}", streamKey);

        try {
            restTemplate.exchange(uri,
                    HttpMethod.POST,
                   null,
                    new ParameterizedTypeReference<Void>() {});
        } catch (HttpStatusCodeException e) {
            log.warn("Exception occurred when transcoding stream {}", streamKey);
            throw e;
        }
    }

    public void stopTranscode(UUID streamKey) {
        URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .pathSegment("transcode")
                .pathSegment(streamKey.toString())
                .build().toUri();

        log.info("Sending request to stop transcoding for stream {}", streamKey);

        try {
            restTemplate.exchange(uri,
                    HttpMethod.DELETE,
                    null,
                    new ParameterizedTypeReference<Void>() {});
        } catch (HttpStatusCodeException e) {
            log.warn("Exception occurred when stopping transcoding stream {}", streamKey);
            throw e;
        }
    }
}
