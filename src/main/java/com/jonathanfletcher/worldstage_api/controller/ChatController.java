package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.model.ChatMessage;
import com.jonathanfletcher.worldstage_api.model.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.security.Principal;

@Slf4j
@Controller
public class ChatController {

    @MessageMapping("/send")
    @SendTo("/chat/messages")
    @PreAuthorize("isAuthenticated()")
    public ChatMessage handleMessage(ChatMessage message) {
        //TODO Figure out how to use principal instead

        if (message.getMessageType() == null) {
            message.setMessageType(MessageType.AUDIENCE);
        }
        message.setTimestamp(Instant.now());
        return message;
    }


}
