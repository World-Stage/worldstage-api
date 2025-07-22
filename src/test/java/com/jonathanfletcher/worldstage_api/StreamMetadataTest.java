package com.jonathanfletcher.worldstage_api;

import com.jonathanfletcher.worldstage_api.model.request.StreamMetadataRequest;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class StreamMetadataTest extends BaseTest {

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
}
