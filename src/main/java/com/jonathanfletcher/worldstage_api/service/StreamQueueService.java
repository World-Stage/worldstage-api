package com.jonathanfletcher.worldstage_api.service;

import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import com.jonathanfletcher.worldstage_api.model.entity.Stream;
import com.jonathanfletcher.worldstage_api.repository.StreamRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public synchronized void  removeStreamFromQueue(Stream stream) {
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

        Stream next = streamQueue.poll();

        if (next != null) {
            currentStream = next;
            currentStream.setStatus(StreamStatus.ACTIVE);
            currentStream.setActive(true);
            streamRepository.save(currentStream);
            log.info("Started new stream: {}", currentStream.getId());

        } else {
            log.info("No other stream in queue. Extending current stream.");
        }
        timerTask = scheduler.schedule(this::onTimerExpired, Instant.now().plusSeconds(15));
    }

    private synchronized void onTimerExpired() {
        log.info("Timer expired for stream: {}", currentStream.getStreamKey());

        if (!streamQueue.isEmpty()) {
            currentStream.setActive(false);
            currentStream.setStatus(StreamStatus.ENDED);
            streamRepository.save(currentStream);
            currentStream = null;
        }

        startNextStream();
    }

    public synchronized void extendCurrentStream(int seconds) {
        if (timerTask != null) {
            timerTask.cancel(false);
        }
        log.info("Extending current stream: {} by {}s", currentStream.getId(), seconds);
        timerTask = scheduler.schedule(this::onTimerExpired, Instant.now().plusSeconds(seconds));
    }

    public Stream getCurrentStream() {
        return currentStream;
    }

    public Queue<Stream> getQueue() {
        return streamQueue;
    }
}
