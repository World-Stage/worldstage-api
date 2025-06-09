package com.jonathanfletcher.worldstage_api.service;

import com.jonathanfletcher.worldstage_api.model.EncoreMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncoreService {

    private final WebSocketViewerTracker webSocketViewerTracker;

    private final Set<UUID> encoreUserIds = ConcurrentHashMap.newKeySet();

    private final SimpMessagingTemplate messagingTemplate;

    public EncoreMetrics castVote(UUID userId) {
        encoreUserIds.add(userId);
        return getEncoreMetrics();
    }

    public EncoreMetrics getEncoreMetrics() {
        int viewerCount = webSocketViewerTracker.getViewerCount();
        int encoreTotal = encoreUserIds.size();
        int progressPercent = 0;
        int encoreNeeded = 0;

        if (viewerCount != 0) {
            int required = (int) Math.ceil(0.51 * viewerCount);
            encoreNeeded = Math.max(0, required - encoreTotal);

            // Progress toward required threshold
            progressPercent = Math.min(100, (int) ((encoreTotal * 100.0) / required));
        }

        return EncoreMetrics.builder()
                .encoreTotal(encoreTotal)
                .encoreNeeded(encoreNeeded)
                .encoreProgressPercent(progressPercent)
                .build();
    }

    public void resetEncore() {
        encoreUserIds.clear();
        messagingTemplate.convertAndSend("/encore", EncoreMetrics.builder()
                .encoreTotal(0)
                .encoreNeeded(null)
                .encoreProgressPercent(0)
                .build()
        );
    }

    public boolean hasUserEncore(UUID userId) {
        return encoreUserIds.contains(userId);
    }
}
