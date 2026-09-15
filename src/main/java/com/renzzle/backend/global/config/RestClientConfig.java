package com.renzzle.backend.global.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    public static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClientCustomizer restClientTimeoutCustomizer() {
        return builder -> builder.requestFactory(ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(CONNECT_TIMEOUT)
                        .withReadTimeout(READ_TIMEOUT)));
    }

}
