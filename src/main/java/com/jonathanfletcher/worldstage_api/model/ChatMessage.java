package com.jonathanfletcher.worldstage_api.model;

import lombok.Data;

import java.time.Instant;

@Data
public class ChatMessage {
    private String sender;
    private String content;
    private Instant timestamp;
}
