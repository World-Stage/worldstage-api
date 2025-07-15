package com.jonathanfletcher.worldstage_api.proxy.property;

import com.jonathanfletcher.worldstage_api.configuration.ProxyProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("proxy.transcoder")
public class TranscoderProxyProperties implements ProxyProperties {

    private String baseUrl;
}
