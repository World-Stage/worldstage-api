package com.jonathanfletcher.worldstage_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathanfletcher.worldstage_api.model.entity.User;
import com.jonathanfletcher.worldstage_api.model.request.AuthRequest;
import com.jonathanfletcher.worldstage_api.model.request.UserCreateRequest;
import com.jonathanfletcher.worldstage_api.model.response.AuthResponse;
import com.jonathanfletcher.worldstage_api.model.response.StreamResponse;
import com.jonathanfletcher.worldstage_api.model.response.UserResponse;
import com.jonathanfletcher.worldstage_api.repository.UserRepository;
import com.jonathanfletcher.worldstage_api.spring.security.JwtUtil;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Collections;
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

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtUtil jwtUtils;

    @Value("${spring.security.client.nginx.secret}")
    protected String nginxSecret;

    @BeforeEach
    public void setUp() {
        RestAssured.port = serverPort;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.config = RestAssured.config().objectMapperConfig(
                new ObjectMapperConfig().jackson2ObjectMapperFactory((type, s) -> objectMapper
                ));
    }

    @AfterEach
    public void strip() {
        userRepository.deleteAll();;
    }

    protected void addAuth(UUID userId) {
        addAuth(userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found to add auth to")));
    }

    protected void addAuth(User user) {
        RestAssured.replaceFiltersWith(Collections.emptyList());
        RestAssured.filters(Arrays.asList(new Filter[] {
                (paramFilterableRequestSpecification, paramFilterableResponseSpecification, paramFilterContext) -> {
                    String token = jwtUtils.generateAccessToken(user);
                    if (!paramFilterableRequestSpecification.getHeaders().hasHeaderWithName("Authorization")) {
                        paramFilterableRequestSpecification.header("Authorization", String.format("Bearer %s", token));
                    }
                    return paramFilterContext.next(paramFilterableRequestSpecification, paramFilterableResponseSpecification);
                }
        }));
    }

    protected StreamResponse publishStream(UUID streamKey) {
        return given()
            .queryParam("name", streamKey)
            .queryParam("secret", nginxSecret)
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

    protected UserResponse createUser() {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("test")
                .email("test@test.com")
                .password("test123")
                .build();

        return given()
                .contentType(ContentType.JSON)
                .body(request)
            .when()
                .post("/auth/register")
            .then()
                .statusCode(HttpStatus.SC_OK)
                .body("id", notNullValue())
                .body("email", equalTo(request.getEmail()))
                .body("username", equalTo(request.getUsername()))
                .extract()
                .as(UserResponse.class);
    }

    protected AuthResponse loginUser(String username) {
        return given()
            .contentType(ContentType.JSON)
            .body(AuthRequest.builder()
                    .username(username)
                    .password("test123")
                    .build())
            .when()
            .post("/auth/login")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("accessToken", notNullValue())
            .extract()
            .as(AuthResponse.class);
    }
}
