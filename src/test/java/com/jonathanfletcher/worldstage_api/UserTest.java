package com.jonathanfletcher.worldstage_api;

import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

public class UserTest extends BaseTest{

    @Test
    void canGetUser() {
        UserResponse user = createUser();
        addAuth(user.getId());

        given()
            .pathParams("userId", user.getId())
        .when()
            .get("/users/{userId}")
        .then()
            .log().all()
            .statusCode(HttpStatus.SC_OK);
    }
}
