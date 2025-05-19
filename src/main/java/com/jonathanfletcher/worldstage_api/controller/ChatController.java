package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.model.ChatMessage;
import lombok.Data;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
public class ChatController {

    @MessageMapping("/send") // from frontend /app/send
    @SendTo("/topic/messages")
    public ChatMessage handleMessage(ChatMessage message) {
        message.setTimestamp(Instant.now());
        return message;
    }


}
