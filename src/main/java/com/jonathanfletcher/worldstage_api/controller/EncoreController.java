package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.model.EncoreRequest;
import com.jonathanfletcher.worldstage_api.model.EncoreMetrics;
import com.jonathanfletcher.worldstage_api.service.EncoreService;
import com.jonathanfletcher.worldstage_api.service.StreamQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.NoSuchElementException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class EncoreController {

    private final EncoreService encoreService;

    private final StreamQueueService streamQueueService;

    @MessageMapping("/encore")
    @SendTo("/topic/encore")
    public EncoreMetrics handleEncore(EncoreRequest request) {
        log.info("User {} is requesting an encore", request.getUserId());

        if (streamQueueService.getCurrentStream() == null) {
            log.info("User {} wants an encore for no active stream!", request.getUserId());
            throw new NoSuchElementException("There is currently no active stream");
        }

        encoreService.castVote(request.getUserId());

        return encoreService.getEncoreMetrics();
    }
}
