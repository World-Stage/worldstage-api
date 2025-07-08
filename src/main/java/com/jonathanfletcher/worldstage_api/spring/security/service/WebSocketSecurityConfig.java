package com.jonathanfletcher.worldstage_api.spring.security.service;

import com.jonathanfletcher.worldstage_api.spring.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

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

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/encore", "/chat");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:80", "http://localhost", "http://localhost:3000")
                .withSockJS();
    }

//    @Override
//    public void configureClientInboundChannel(ChannelRegistration registration) {
//        registration.interceptors(new ChannelInterceptor() {
//            @Override
//            public Message<?> preSend(Message<?> message, MessageChannel channel) {
//                // Allow all messages to pass through without authentication
//                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
//                if (StompCommand.CONNECT.equals(accessor.getCommand()) ||
//                        StompCommand.SUBSCRIBE.equals(accessor.getCommand()) ||
//                        StompCommand.DISCONNECT.equals(accessor.getCommand())) {
//                    // Set an anonymous authentication token for WebSocket connections
//                    Authentication auth = new AnonymousAuthenticationToken(
//                            "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
//                    SecurityContextHolder.getContext().setAuthentication(auth);
//                }
//                return message;
//            }
//        });
//    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                StompCommand command = accessor.getCommand();
                String sessionId = accessor.getSessionId();

                if (StompCommand.CONNECT.equals(command)) {
                    log.info("Processing CONNECT for session: {}", sessionId);
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        if (jwtUtil.validateToken(token, false)) {
                            String username = jwtUtil.getUsernameFromToken(token, false);
                            Set<String> roles = jwtUtil.getRolesFromToken(token);
                            if (username != null) {
                                var authorities = roles.stream()
                                        .map(SimpleGrantedAuthority::new)
                                        .collect(Collectors.toList());
                                Authentication auth = new UsernamePasswordAuthenticationToken(
                                        username, null, authorities);
                                accessor.setUser(auth);
                                SecurityContextHolder.getContext().setAuthentication(auth);
                                log.info("Authenticated user: {} for session: {}", username, sessionId);
                            } else {
                                log.warn("Invalid username from token for session: {}", sessionId);
                                setAnonymousAuthentication(accessor);
                            }
                        } else {
                            log.warn("Invalid JWT token for session: {}", sessionId);
                            setAnonymousAuthentication(accessor);
                        }
                    } else {
                        log.info("No Authorization header, setting anonymous for session: {}", sessionId);
                        setAnonymousAuthentication(accessor);
                    }
                } else if (StompCommand.SUBSCRIBE.equals(command)) {
                    String destination = accessor.getDestination();
                    log.info("Processing SUBSCRIBE to {} for session: {}", destination, sessionId);
                    if ("/chat/viewers".equals(destination) || "/chat/messages".equals(destination) || "/encore".equals(destination)) {
                        return message; // Allow anonymous subscriptions to these destinations
                    }
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth == null || auth instanceof AnonymousAuthenticationToken) {
                        log.warn("Denied SUBSCRIBE to {}: Authentication required", destination);
                        throw new SecurityException("Authentication required for subscription to " + destination);
                    }
                } else if (StompCommand.SEND.equals(command)) {
                    String destination = accessor.getDestination();
                    log.info("Processing SEND to {} for session: {}", destination, sessionId);
                    if ("/app/send".equals(destination) || "/app/encore".equals(destination)) {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
                            log.warn("Denied SEND to {}: Authentication required", destination);
                            throw new SecurityException("Authentication required for sending to " + destination);
                        }
                    }
                } else if (StompCommand.DISCONNECT.equals(command)) {
                    log.info("Processing DISCONNECT for session: {}", sessionId);
                    return message;
                }

                return message;
            }

            private void setAnonymousAuthentication(StompHeaderAccessor accessor) {
                Authentication auth = new AnonymousAuthenticationToken(
                        "anonymous", "anonymousUser",
                        java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
                accessor.setUser(auth);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("Set anonymous authentication for session: {}", accessor.getSessionId());
            }
        });
    }
}