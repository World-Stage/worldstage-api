package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping(path = "/stream/view") //TODO Probably change view to active as we want to subscribe to active
public class StreamSseController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        log.info("New SSE subscriber: total now {}", emitters.size());
        return emitter;
    }

    public void notifyNewActiveStream(StreamResponse streamResponse) {
        log.info("Sending SSE for new active stream to {} subscribers", emitters.size());
        emitMessage("new-stream", streamResponse);
    }

    public void notifyUpdatedTimer(Instant expiration) {
        log.info("Sending SSE for updated stream expiration {}", expiration.getEpochSecond());
        emitMessage("stream-expiration", expiration.getEpochSecond());
    }

    private <T> void emitMessage(String name, T data) {
        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(name)
                        .data(data));
            } catch (IOException e) {
                log.warn("Removing {} dead emitter", deadEmitters.size());
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }
}
