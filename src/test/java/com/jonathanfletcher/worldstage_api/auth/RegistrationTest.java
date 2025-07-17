package com.jonathanfletcher.worldstage_api.auth;

import com.jonathanfletcher.worldstage_api.BaseTest;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.model.request.AuthRequest;
import com.jonathanfletcher.worldstage_api.model.request.UserCreateRequest;
import com.jonathanfletcher.worldstage_api.model.response.AuthResponse;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import com.jonathanfletcher.worldstage_api.repository.UserRepository;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.Optional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class RegistrationTest extends BaseTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void canRegisterUser() {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("test")
                .email("test@test.com")
                .password("test123")
                .build();

        AuthResponse response = given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/auth/register")
            .then()
                .statusCode(HttpStatus.SC_OK)
                .body("user.id", notNullValue())
                .body("user.email", equalTo(request.getEmail()))
                .body("user.username", equalTo(request.getUsername()))
            .extract()
            .as(AuthResponse.class);

        Optional<User> createdUser = userRepository.findById(response.getUser().getId());
        Assert.isTrue(createdUser.isPresent(), "User not found");
    }

    @ParameterizedTest
    @ValueSource(strings = {"test@test.com", "test"})
    void canLoginUser(String username) {
        createUser();

        given()
            .contentType(ContentType.JSON)
            .body(AuthRequest.builder()
                    .username(username)
                    .password("test123")
                    .build())
        .when()
            .post("/auth/login")
        .then()
            .statusCode(HttpStatus.SC_OK)
            .body("accessToken", notNullValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"test@test.com", "test"})
    void cannotCreateUserWithSameUsername() {
        createUser();

        UserCreateRequest request = UserCreateRequest.builder()
                .username("test")
                .email("test@test.com")
                .password("test123")
                .build();

        given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/auth/register")
            .then()
                .statusCode(HttpStatus.SC_CONFLICT);
    }
}
