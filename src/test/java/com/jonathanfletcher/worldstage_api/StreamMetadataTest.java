package com.jonathanfletcher.worldstage_api;

import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import com.jonathanfletcher.worldstage_api.model.request.StreamMetadataRequest;
import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import com.jonathanfletcher.worldstage_api.repository.StreamRepository;
import com.jonathanfletcher.worldstage_api.service.StreamQueueService;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class StreamMetadataTest extends BaseTest {

    @Autowired
    private StreamRepository streamRepository;

    @Autowired
    private StreamQueueService streamQueueService;

    @BeforeEach
    void resetState() {
        // Clear all DB entries
        streamRepository.deleteAll();

        // Clear the queue and reset the current stream
        streamQueueService.getQueue().clear();

        // Use reflection to reset the currentStream and cancel the timer
        try {
            var currentStreamField = StreamQueueService.class.getDeclaredField("currentStream");
            currentStreamField.setAccessible(true);
            currentStreamField.set(streamQueueService, null);

            var timerTaskField = StreamQueueService.class.getDeclaredField("timerTask");
            timerTaskField.setAccessible(true);
            var task = (java.util.concurrent.ScheduledFuture<?>) timerTaskField.get(streamQueueService);
            if (task != null) task.cancel(false);
            timerTaskField.set(streamQueueService, null);

        } catch (Exception e) {
            throw new RuntimeException("Failed to reset StreamQueueService", e);
        }
    }

    @Test
    void canUpdateStreamMetadata() {
        UserResponse user = createUser().getUser();
        addAuth(user.getId());
        StreamMetadataRequest request = StreamMetadataRequest.builder()
                .title("New Title")
                .build();

        given()
            .pathParams("userId", user.getId())
            .body(request)
            .contentType(ContentType.JSON)
        .when()
            .patch("/users/{userId}/streamMetadata")
        .then()
            .log().all()
            .statusCode(HttpStatus.SC_OK)
            .body("title", equalTo(request.getTitle()));
    }

    @Test
    void cannotUpdateStreamMetadataOfOtherUser() {
        UserResponse user = createUser().getUser();
        addAuth(user.getId());
        StreamMetadataRequest request = StreamMetadataRequest.builder()
                .title("New Title")
                .build();

        given()
            .pathParams("userId", UUID.randomUUID())
            .body(request)
            .contentType(ContentType.JSON)
        .when()
            .patch("/users/{userId}/streamMetadata")
        .then()
            .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    void updatingStreamMetadataUpdatesActiveStream() {
        UserResponse user = createUser().getUser();
        addAuth(user.getId());
        StreamResponse activeStream = publishStream(user.getStreamKey());

        StreamMetadataRequest request = StreamMetadataRequest.builder()
                .title("New Title")
                .build();

        given()
            .pathParams("userId", user.getId())
            .body(request)
            .contentType(ContentType.JSON)
        .when()
            .patch("/users/{userId}/streamMetadata")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("title", equalTo(request.getTitle()));

        when()
            .get("/stream/view/active")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", notNullValue())
            .body("streamKey", equalTo(activeStream.getStreamKey().toString()))
            .body("rtmpUrl", notNullValue())
            .body("hlsUrl", notNullValue())
            .body("status", equalTo(StreamStatus.ACTIVE.toString()))
            .body("title", equalTo(request.getTitle()));
    }
}
