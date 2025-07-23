package com.jonathanfletcher.worldstage_api;

import com.jonathanfletcher.worldstage_api.model.StreamStatus;
import com.jonathanfletcher.worldstage_api.model.response.AuthResponse;
import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.*;

public class UserTest extends BaseTest {

    @Test
    void canGetUser() {
        UserResponse user = createUser().getUser();
        addAuth(user.getId());

        given()
            .pathParams("userId", user.getId())
        .when()
            .get("/users/{userId}")
        .then()
                .log().all()
            .statusCode(HttpStatus.SC_OK)
            .body("id", equalTo(user.getId().toString()))
            .body("email", equalTo(user.getEmail()))
            .body("username", equalTo(user.getUsername()))
            .body("activeStream", nullValue())
            .body("streamMetadata.title", notNullValue());
    }

    @Test
    void canGetUserFromAuth() {
        AuthResponse response = createUser();
        UserResponse user = response.getUser();
        addAuth(user.getId());

        when()
            .get("/auth/me")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", equalTo(user.getId().toString()))
            .body("email", equalTo(user.getEmail()))
            .body("username", equalTo(user.getUsername()));
    }

    @Test
    void canGetUserActiveStream() {
        UserResponse user = createUser().getUser();
        addAuth(user.getId());
        publishStream(user.getStreamKey());

        given()
            .pathParams("userId", user.getId())
        .when()
            .get("/users/{userId}")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", equalTo(user.getId().toString()))
            .body("username", equalTo(user.getUsername()))
            .body("activeStream.streamKey", equalTo(user.getStreamKey().toString()))
            .body("activeStream.active", equalTo(true));
    }

    @Test
    void cannotGetActiveStreamIfNoneExists() {
        UserResponse user = createUser().getUser();
        addAuth(user.getId());

        given()
            .pathParams("userId", user.getId())
            .queryParam("returnActiveStream", true)
        .when()
            .get("/users/{userId}")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", equalTo(user.getId().toString()))
            .body("username", equalTo(user.getUsername()))
            .body("activeStream", nullValue());
    }

    @Test
    void canRegenerateStreamKey() {
        UserResponse user = createUser().getUser();
        addAuth(user.getId());

        given()
            .pathParams("userId", user.getId())
        .when()
            .post("/users/{userId}/regenerateStreamKey")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("streamKey", not(user.getStreamKey().toString()));
    }

    @Test
    void cannotRegenerateOtherUsersKey() {
        UserResponse user = createUser().getUser();
        addAuth(user.getId());

        given()
            .pathParams("userId", UUID.randomUUID())
        .when()
            .post("/users/{userId}/regenerateStreamKey")
        .then()
            .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    void updatingStreamKeyWhenLiveUnpublishesStream() {
        UserResponse user = createUser().getUser();
        addAuth(user.getId());
        StreamResponse stream = publishStream(user.getStreamKey());

        given()
            .pathParams("userId", user.getId())
        .when()
            .post("/users/{userId}/regenerateStreamKey")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("streamKey", not(user.getStreamKey().toString()));

        when()
            .get("/streams/view/active")
        .then()
            .statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
