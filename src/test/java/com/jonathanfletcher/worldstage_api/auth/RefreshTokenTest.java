package com.jonathanfletcher.worldstage_api.auth;

import com.jonathanfletcher.worldstage_api.BaseTest;
import com.jonathanfletcher.worldstage_api.model.response.AuthResponse;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import com.jonathanfletcher.worldstage_api.spring.security.JwtUtil;
import com.jonathanfletcher.worldstage_api.spring.security.service.TokenService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RefreshTokenTest extends BaseTest {

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    TokenService tokenService;

    private String validRefreshToken;
    private UUID validFamilyId;
    private String validCsrfToken;
    private UserResponse user;

    @BeforeEach
    public void setup() {
        user = createUser();
        validFamilyId = UUID.randomUUID();
        validRefreshToken = jwtUtil.generateRefreshToken(user.getUsername(), validFamilyId);
        tokenService.storeRefreshToken(validRefreshToken, user.getUsername(), validFamilyId);

//        validCsrfToken = given()
//                .when()
//                .get("/auth/csrf")
//                .then()
//                .statusCode(HttpStatus.SC_OK)
//                .log().all()
//                .extract()
//                .cookie("XSRF-TOKEN");
    }

    @Test
    void canGetNewAccessTokenFromRefresh() {
        AuthResponse response = given()
                .cookie("refreshToken", validRefreshToken)
//                .cookie("XSRF-TOKEN", validCsrfToken)  // ✅ send CSRF token as cookie
//                .header("X-CSRF-TOKEN", validCsrfToken)
                .when()
                .post("/auth/refresh")
                .then()
                .log().all()
                .statusCode(HttpStatus.SC_OK)
                .body("accessToken", notNullValue())
                .extract().as(AuthResponse.class);

        String newUsername = jwtUtil.getUsernameFromToken(response.getAccessToken(), false);
        assertNotNull(newUsername);
        assertEquals(user.getUsername(), newUsername);
    }

    @Test
    void cannotGetNewAccessTokenFromInvalidRefresh() {
        String test = "test";
        given()
            .cookie("refreshToken", "eyJhbGciOiJIUzM4NCJ9.eyJyb2xlcyI6WyJVU0VSIl0sInN1YiI6InRlc3QiLCJpYXQiOjE3NTE2NzYwODAsImV4cCI6MTc1MTY3Njk4MH0.YlOFyMJ7IuBU9qi5t2Dra5rDFw4P-Wc3TF1NsKrMtnAIPe07tJacmRvShZiwgw69")
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    void cannotGetNewAccessTokenFromExpiredRefresh() {
        String token = jwtUtil.generateExpiredRefresh(user.getUsername(), UUID.randomUUID());

        given()
            .cookie("refreshToken", token)
        .when()
            .post("/auth/refresh")
        .then()
            .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}
