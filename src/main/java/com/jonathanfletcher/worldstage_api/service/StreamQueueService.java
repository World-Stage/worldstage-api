package com.jonathanfletcher.worldstage_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathanfletcher.worldstage_api.controller.StreamSseController;
import com.jonathanfletcher.worldstage_api.model.ChatMessage;
import com.jonathanfletcher.worldstage_api.model.MessageType;
import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import com.jonathanfletcher.worldstage_api.model.entity.Stream;
import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import com.jonathanfletcher.worldstage_api.repository.StreamRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamQueueService {

    private final StreamRepository streamRepository;

    private final Queue<Stream> streamQueue = new ConcurrentLinkedQueue<>();
    private Stream currentStream;
    private ScheduledFuture<?> timerTask;

    private TaskScheduler scheduler;

    private final StreamSseController streamSseController;

    private final ObjectMapper objectMapper;

    private final SimpMessagingTemplate messagingTemplate;

    private final EncoreService encoreService;

    private int extensionLevel = 0;

    private Integer encoreExtensionTime;

    private static final int[] extensionSteps = {30, 60, 120, 240}; // Seconds
    private static final int maxExtension = 300;

    @PostConstruct
    public void init() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.initialize();
        this.scheduler = threadPoolTaskScheduler;
    }

    public synchronized void addStreamToQueue(Stream stream) {
        streamQueue.add(stream);
        log.info("Added stream to queue: {}", stream.getId());
        if (currentStream == null) {
            startNextStream();
        }
    }

    public synchronized void removeStreamFromQueue(Stream stream) {
        try {
            streamQueue.remove(stream);
            if (stream.getId().equals(currentStream.getId()) && !streamQueue.isEmpty()) {
                // start next stream if current stream disconnects
                startNextStream();
            } else if (stream.getId().equals(currentStream.getId())) {
                // Current stream is the one we are ending and no stream in queue, cancel timer
                log.info("No stream in queue to replace current stream, cancelling timer");
                timerTask.cancel(true);
                currentStream = null;
            }
            log.info("Successfully removed stream {} from queue", stream.getId());
        } catch (Exception e) {
            log.error("Unable to remove stream {} from queue", stream.getId());
            throw e;
        }
    }

    private synchronized void startNextStream() {
        if (timerTask != null) {
            timerTask.cancel(false);
        }
        // Reset Encore Metrics for next stream
        encoreService.resetEncore();
        extensionLevel = 0;
        encoreExtensionTime = null; //Reset extension time

        Stream next = streamQueue.poll();
        if (next != null) {
            currentStream = next;
            currentStream.setStatus(StreamStatus.ACTIVE);
            currentStream.setActive(true);
            streamRepository.save(currentStream);
            log.info("Started new stream: {}", currentStream.getId());
            streamSseController.notifyNewActiveStream(objectMapper.convertValue(next, StreamResponse.class));


            // 🟢 Send system chat message
            ChatMessage systemMessage = ChatMessage.builder()
                    .content("Starting next stream")
                    .messageType(MessageType.SYSTEM)
                    .timestamp(Instant.now())
                    .build();
            messagingTemplate.convertAndSend("/chat/messages", systemMessage);
        } else {
            log.info("No other stream in queue. Extending current stream.");
        }

        Instant newStreamExpiration = Instant.now().plusSeconds(15); //TODO Calculate this
        updateStreamTimer(newStreamExpiration);
    }

    private synchronized void onTimerExpired() {
        log.info("Timer expired for stream: {}", currentStream.getStreamKey());
        if (encoreExtensionTime != null) {
            if (timerTask != null) {
                timerTask.cancel(false);
            }
            Instant newStreamExpiration = Instant.now().plusSeconds(encoreExtensionTime);
            encoreExtensionTime = null; //Reset extension time
            updateStreamTimer(newStreamExpiration);
            encoreService.resetEncore();
        } else {
            if (!streamQueue.isEmpty()) {
                currentStream.setActive(false);
                currentStream.setStatus(StreamStatus.ENDED);
                streamRepository.save(currentStream);
                currentStream = null;
            }

            startNextStream();
        }
    }

    private synchronized void updateStreamTimer(Instant extensionTime) {
        timerTask = scheduler.schedule(this::onTimerExpired, extensionTime);
        streamSseController.notifyUpdatedTimer(extensionTime);
    }

    public synchronized void extendCurrentStream() {
        // Calculate extension time based on exponential backoff, capped at 5 minutes
        int extensionSeconds;
        if (extensionLevel < extensionSteps.length) {
            extensionSeconds = extensionSteps[extensionLevel];
        } else {
            extensionSeconds = maxExtension;
        }

        extensionLevel++; // Increase for next round

        log.info("Extending current stream: {} by {}s", currentStream.getId(), extensionSeconds);
        encoreExtensionTime = extensionSeconds;
    }

    public Stream getCurrentStream() {
        return currentStream;
    }

    public Queue<Stream> getQueue() {
        return streamQueue;
    }
}
