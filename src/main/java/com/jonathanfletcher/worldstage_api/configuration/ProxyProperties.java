package com.jonathanfletcher.worldstage_api.configuration;

import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

@Validated
public interface ProxyProperties {

    @NotNull
    String getBaseUrl();
}
