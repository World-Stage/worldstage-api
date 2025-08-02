package com.jonathanfletcher.worldstage_api.spring.security.service;

import com.jonathanfletcher.worldstage_api.property.CorsProperties;
import com.jonathanfletcher.worldstage_api.spring.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    public WebSocketSecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Autowired
    private CorsProperties corsProperties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/encore", "/chat");
        registry.setUserDestinationPrefix("/user");
        log.info("Message broker configured with prefixes: /app, /encore, /chat, /user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(new String[0]))
                .withSockJS();
        log.info("WebSocket endpoint registered: /ws");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                StompCommand command = accessor.getCommand();
                String sessionId = accessor.getSessionId();

                log.debug("Processing command: {} for session: {} on thread: {}",
                        command, sessionId, Thread.currentThread().getId());

                if (StompCommand.CONNECT.equals(command) ||
                        StompCommand.SEND.equals(command) ||
                        StompCommand.SUBSCRIBE.equals(command)) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    log.debug("Authorization header: {} for command: {}", authHeader, command);

                    Authentication auth;
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        if (jwtUtil.validateToken(token, false)) {
                            String username = jwtUtil.getUsernameFromToken(token, false);
                            Set<String> roles = jwtUtil.getRolesFromToken(token);
                            if (username != null) {
                                var authorities = roles.stream()
                                        .map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toList());
                                auth = new UsernamePasswordAuthenticationToken(
                                        username, null, authorities);
                                log.info("Authenticated user: {} with roles: {} for session: {}",
                                        username, roles, sessionId);
                            } else {
                                log.warn("Invalid username from token for session: {}", sessionId);
                                auth = createAnonymousAuthentication();
                            }
                        } else {
                            log.warn("Invalid JWT token for session: {}", sessionId);
                            auth = createAnonymousAuthentication();
                        }
                    } else {
                        log.info("No Authorization header, setting anonymous for session: {}", sessionId);
                        auth = createAnonymousAuthentication();
                    }

                    // Set the Principal for the message
                    accessor.setUser(auth);

                    // Authorization checks
                    if (StompCommand.SUBSCRIBE.equals(command)) {
                        String destination = accessor.getDestination();
                        log.info("Processing SUBSCRIBE to {} for session: {}", destination, sessionId);
                        if ("/chat/viewers".equals(destination) || "/chat/messages".equals(destination) || "/encore".equals(destination)) {
                            return message; // Allow public subscriptions
                        }
                        if (auth instanceof AnonymousAuthenticationToken) {
                            log.warn("Denied SUBSCRIBE to {}: Authentication required", destination);
                            throw new SecurityException("Authentication required for subscription to " + destination);
                        }
                    } else if (StompCommand.SEND.equals(command)) {
                        String destination = accessor.getDestination();
                        log.info("Processing SEND to {} for session: {}", destination, sessionId);
                        if ("/app/send".equals(destination) || "/app/encore".equals(destination)) {
                            if (auth instanceof AnonymousAuthenticationToken) {
                                log.warn("Denied SEND to {}: Authentication required", destination);
                                throw new SecurityException("Authentication required for sending to " + destination);
                            }
                            log.info("SEND allowed for user: {} to {}", auth.getName(), destination);
                        }
                    }
                } else if (StompCommand.DISCONNECT.equals(command)) {
                    log.info("Processing DISCONNECT for session: {}", sessionId);
                }

                return message;
            }

            private Authentication createAnonymousAuthentication() {
                return new AnonymousAuthenticationToken(
                        "anonymous", "anonymousUser",
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
            }
        });
    }
}