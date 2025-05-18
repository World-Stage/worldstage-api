package com.jonathanfletcher.worldstage_api;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

public class StreamTest extends BaseTest {


    @Test
    void canPublishStream() {
        given()
                .queryParam("name", "test123")
        .when()
            .post("/stream/publish")
        .then()
            .statusCode(HttpStatus.SC_OK);
    }

    @Test
    void canGetActiveStream() {
        given()
            .queryParam("name", "test123")
        .when()
            .post("/stream/publish")
        .then()
            .statusCode(HttpStatus.SC_OK);

        when()
            .get("/stream/view/active")
        .then()
            .log().all()
            .statusCode(HttpStatus.SC_OK);
    }
}
