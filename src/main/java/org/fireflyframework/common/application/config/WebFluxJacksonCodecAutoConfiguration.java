/*
 * Copyright 2024-2026 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */

package org.fireflyframework.common.application.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Wires the WebFlux server-side Jackson codec to the Spring Boot-autoconfigured
 * {@link ObjectMapper} for experience-tier applications.
 *
 * <p><strong>Why this exists:</strong> when an experience service declares
 * {@code @EnableWebFlux} on its main class (the convention across the platform),
 * Spring Boot's {@code WebFluxAutoConfiguration} is disabled — and with it goes
 * the path that consumes {@code CodecCustomizer} beans (like Boot's own
 * {@code CodecsAutoConfiguration#jacksonCodecCustomizer}) and applies them to the
 * {@code ServerCodecConfigurer}. The result: the WebFlux Jackson encoder falls
 * back to a default {@code Jackson2JsonEncoder()} built from a fresh
 * {@code ObjectMapper} with default settings — no {@code spring.jackson.*}
 * properties applied (so {@code WRITE_DATES_AS_TIMESTAMPS=true} stays on), no
 * actuator {@code @JsonComponent} serializers, no application-specific Jackson
 * modules. Every {@code Instant} field serializes as a numeric epoch and every
 * {@code LocalDateTime} as a numeric array, instead of ISO-8601 strings.
 *
 * <p>The {@code core-tier} starter has its own {@code WebFluxAutoConfiguration}
 * that solves the same problem via the {@link WebFluxConfigurer} extension point
 * (which {@code DelegatingWebFluxConfiguration}, imported by {@code @EnableWebFlux},
 * DOES discover). This class mirrors that pattern for the experience tier but
 * builds the codec mapper from {@link Jackson2ObjectMapperBuilder} — Boot's
 * autoconfigured builder — so it picks up {@code spring.jackson.*} properties
 * AND every Jackson module on the classpath ({@code jsr310}, {@code jdk8},
 * parameter-names, actuator {@code @JsonComponent} serializers, …) in one shot.
 */
@AutoConfiguration
@ConditionalOnClass({ WebFluxConfigurer.class, Jackson2ObjectMapperBuilder.class })
public class WebFluxJacksonCodecAutoConfiguration implements WebFluxConfigurer {

    private final Jackson2ObjectMapperBuilder objectMapperBuilder;

    public WebFluxJacksonCodecAutoConfiguration(Jackson2ObjectMapperBuilder objectMapperBuilder) {
        this.objectMapperBuilder = objectMapperBuilder;
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        ObjectMapper mapper = objectMapperBuilder.build();
        MediaType applicationStarJson = MediaType.parseMediaType("application/*+json");
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(
                mapper,
                MediaType.APPLICATION_JSON,
                applicationStarJson,
                MediaType.APPLICATION_NDJSON));
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(
                mapper,
                MediaType.APPLICATION_JSON,
                applicationStarJson,
                MediaType.APPLICATION_NDJSON));
    }
}
