package com.jonathanfletcher.worldstage_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public abstract class BaseTest {

    @LocalServerPort
    private int serverPort;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        RestAssured.port = serverPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.config = RestAssured.config().objectMapperConfig(
                new ObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> objectMapper
                ));
    }

    protected StreamResponse publishStream(UUID streamKey) {
        return given()
            .queryParam("name", streamKey)
        .when()
            .post("/stream/publish")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", notNullValue())
            .body("streamKey", equalTo(streamKey.toString()))
            .body("rtmpUrl", notNullValue())
            .body("hlsUrl", notNullValue())
            .extract()
            .as(StreamResponse.class);
    }

    protected StreamResponse getActiveStream() {
        return when()
                .get("/stream/view/active")
            .then()
                .statusCode(HttpStatus.SC_OK)
                .body("id", notNullValue())
                .body("streamKey", notNullValue())
                .body("rtmpUrl", notNullValue())
                .body("hlsUrl", notNullValue())
                .extract()
                .as(StreamResponse.class);
    }
}
