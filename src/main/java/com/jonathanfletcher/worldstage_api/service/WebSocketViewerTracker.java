package com.jonathanfletcher.worldstage_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketViewerTracker {

    private final Set<String> sessionIds = ConcurrentHashMap.newKeySet();

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        log.info("CONNECTED: {}", event.getMessage());
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        sessionIds.add(sessionId);
        broadcastViewerCount();
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();
        sessionIds.remove(sessionId);
        broadcastViewerCount();
    }

    public int getViewerCount() {
        return sessionIds.size();
    }

    private void broadcastViewerCount() {
        messagingTemplate.convertAndSend("/chat/viewers", sessionIds.size());
    }
}
