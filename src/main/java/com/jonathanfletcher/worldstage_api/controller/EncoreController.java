package com.jonathanfletcher.worldstage_api.controller;

import com.jonathanfletcher.worldstage_api.exception.EntityNotFoundException;
import com.jonathanfletcher.worldstage_api.model.EncoreRequest;
import com.jonathanfletcher.worldstage_api.model.EncoreMetrics;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.repository.UserRepository;
import com.jonathanfletcher.worldstage_api.service.EncoreService;
import com.jonathanfletcher.worldstage_api.service.StreamQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class EncoreController {

    private final EncoreService encoreService;

    private final StreamQueueService streamQueueService;

    private final UserRepository userRepository;

    @MessageMapping("/encore")
    @SendTo("/encore")
    @PreAuthorize("isAuthenticated()")
    public EncoreMetrics handleEncore(Principal principal) {
        String username = principal.getName(); // the authenticated username
        // TODO Figure out a way to not load user everytime
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("Could not find user"));

        log.info("User {} is requesting an encore", username);

        if (streamQueueService.getCurrentStream() == null) {
            log.info("User {} wants an encore for no active stream!", username);
            throw new NoSuchElementException("There is currently no active stream");
        }

        EncoreMetrics metrics = encoreService.castVote(UUID.fromString(username));

        if (metrics.getEncoreProgressPercent() >= 100) {
            log.info("Encore progression reached. Extending current stream");
            streamQueueService.extendCurrentStream();
        }

        return metrics;
    }
}
