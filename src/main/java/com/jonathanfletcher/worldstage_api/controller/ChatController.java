package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.model.ChatMessage;
import com.jonathanfletcher.worldstage_api.model.MessageType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.security.Principal;

@Controller
public class ChatController {

    @MessageMapping("/send") // from frontend /app/send
    @SendTo("/chat/messages")
    public ChatMessage handleMessage(ChatMessage message, Principal principal) {
        if (principal == null) {
            throw new SecurityException("User must be authenticated to send messages.");
        }

        if (message.getMessageType() == null) {
            message.setMessageType(MessageType.AUDIENCE);
        }

        message.setSender(principal.getName());
        message.setTimestamp(Instant.now());
        return message;
    }


}
