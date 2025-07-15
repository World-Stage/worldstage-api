package com.jonathanfletcher.worldstage_api;

import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import com.jonathanfletcher.worldstage_api.proxy.mock.MockTranscoderController;
import com.jonathanfletcher.worldstage_api.repository.StreamRepository;
import com.jonathanfletcher.worldstage_api.service.StreamQueueService;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class StreamTest extends BaseTest {


    @Autowired
    private StreamRepository streamRepository;

    @Autowired
    private StreamQueueService streamQueueService;

    @MockitoSpyBean
    MockTranscoderController mockTranscoderController;

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
    void canPublishStream() {
        UUID streamKey = UUID.randomUUID();
        given()
            .queryParam("name", streamKey)
            .queryParam("secret", nginxSecret)
        .when()
            .post("/stream/publish")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", notNullValue())
            .body("streamKey", equalTo(streamKey.toString()))
            .body("rtmpUrl", notNullValue())
            .body("hlsUrl", notNullValue());

        Mockito.verify(mockTranscoderController).createEvent(streamKey);
    }

    @Test
    void canUnPublishStream() {
        StreamResponse activeStream = publishStream(UUID.randomUUID());

        when()
            .get("/stream/view/active")
        .then()
            .statusCode(HttpStatus.SC_OK)
        .body("id", notNullValue())
        .body("streamKey", equalTo(activeStream.getStreamKey().toString()))
        .body("rtmpUrl", notNullValue())
        .body("hlsUrl", notNullValue())
        .body("status", equalTo(StreamStatus.ACTIVE.toString()));

        given()
            .queryParam("name", activeStream.getStreamKey())
            .queryParam("secret", nginxSecret)
        .when()
            .post("/stream/unpublish")
        .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        when()
            .get("/stream/view/active")
        .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void canGetActiveStream() {
        StreamResponse stream = publishStream(UUID.randomUUID());

        when()
            .get("/stream/view/active")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", notNullValue())
            .body("streamKey", equalTo(stream.getStreamKey().toString()))
            .body("rtmpUrl", notNullValue())
            .body("hlsUrl", notNullValue())
            .body("status", equalTo(StreamStatus.ACTIVE.toString()));
    }

    @Test
    void canGetSpecificStream() {
        StreamResponse stream = publishStream(UUID.randomUUID());

        given()
            .pathParams("streamId", stream.getId())
        .when()
            .get("/stream/{streamId}")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", equalTo(stream.getId().toString()))
            .body("streamKey", equalTo(stream.getStreamKey().toString()))
            .body("active", equalTo(true))
            .body("status", equalTo(StreamStatus.ACTIVE.toString()));
    }

    @Test
    void cannotGetNonExistingStream() {
        given()
            .pathParams("streamId", UUID.randomUUID())
        .when()
            .get("/stream/{streamId}")
        .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void streamShouldSwitchAfterExpiration() {
        StreamResponse stream = publishStream(UUID.randomUUID());
        StreamResponse stream2 = publishStream(UUID.randomUUID());

        when()
            .get("/stream/view/active")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", notNullValue())
            .body("streamKey", equalTo(stream.getStreamKey().toString()))
            .body("rtmpUrl", notNullValue())
            .body("hlsUrl", notNullValue())
            .body("status", equalTo(StreamStatus.ACTIVE.toString()));

        await().atMost(20, TimeUnit.SECONDS).until(() ->
            getActiveStream().getStreamKey().equals(stream2.getStreamKey())
        );
    }

    @SneakyThrows
    @Test
    void streamShouldNotSwitchIfNoneInQueue() {
        StreamResponse stream = publishStream(UUID.randomUUID());

        when()
            .get("/stream/view/active")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", notNullValue())
            .body("streamKey", equalTo(stream.getStreamKey().toString()))
            .body("rtmpUrl", notNullValue())
            .body("hlsUrl", notNullValue())
            .body("status", equalTo(StreamStatus.ACTIVE.toString()));

        Thread.sleep(17000);

        when()
            .get("/stream/view/active")
        .then()
        .statusCode(HttpStatus.SC_OK)
            .body("id", notNullValue())
            .body("streamKey", equalTo(stream.getStreamKey().toString()))
            .body("rtmpUrl", notNullValue())
            .body("hlsUrl", notNullValue())
            .body("status", equalTo(StreamStatus.ACTIVE.toString()));
    }
}
