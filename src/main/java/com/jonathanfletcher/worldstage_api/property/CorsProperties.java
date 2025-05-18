package com.jonathanfletcher.worldstage_api.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {

    private boolean enabled;

    private List<String> allowedOrigins = List.of("*");

    private List<String> allowedHeaders = List.of();

    private List<String> exposedHeaders = List.of();

    private Integer maxAge = 3600;
}
