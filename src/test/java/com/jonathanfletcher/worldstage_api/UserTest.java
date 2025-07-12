package com.jonathanfletcher.worldstage_api;

import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

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
            .statusCode(HttpStatus.SC_OK)
            .body("id", equalTo(user.getId().toString()))
            .body("email", equalTo(user.getEmail()))
            .body("username", equalTo(user.getUsername()));
    }

    @Test
    void canGetUserFromAuth() {
        UserResponse user = createUser();
        addAuth(user.getId());

        when()
            .get("/auth/me")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("id", equalTo(user.getId().toString()))
            .body("email", equalTo(user.getEmail()))
            .body("username", equalTo(user.getUsername()));
    }
}
