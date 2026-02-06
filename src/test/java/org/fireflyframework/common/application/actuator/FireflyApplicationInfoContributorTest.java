/*
 * Copyright 2024-2026 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fireflyframework.application.actuator;

import org.fireflyframework.application.context.AppMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FireflyApplicationInfoContributor}.
 */
@DisplayName("FireflyApplicationInfoContributor Tests")
class FireflyApplicationInfoContributorTest {
    
    private FireflyApplicationInfoContributor infoContributor;
    
    @Test
    @DisplayName("Should contribute complete application metadata")
    void shouldContributeCompleteMetadata() {
        // Given
        Map<String, String> buildInfo = new HashMap<>();
        buildInfo.put("git.commit", "abc123");
        buildInfo.put("git.branch", "main");
        
        Set<String> owners = new HashSet<>(Arrays.asList("owner1@getfirefly.io", "owner2@getfirefly.io"));
        Set<String> services = new HashSet<>(Arrays.asList("service-1", "service-2"));
        Set<String> capabilities = new HashSet<>(Arrays.asList("capability-1", "capability-2"));
        
        AppMetadata metadata = AppMetadata.builder()
                .name("test-service")
                .displayName("Test Service")
                .version("1.0.0")
                .description("Test description")
                .domain("test-domain")
                .team("test-team")
                .owners(owners)
                .apiBasePath("/api/v1/test")
                .usesServices(services)
                .capabilities(capabilities)
                .documentationUrl("https://docs.test.com")
                .repositoryUrl("https://github.com/test/repo")
                .deprecated(false)
                .environment("prod")
                .startupTime(Instant.now())
                .buildInfo(buildInfo)
                .customProperties(new HashMap<>())
                .build();
        
        infoContributor = new FireflyApplicationInfoContributor(metadata);
        Info.Builder builder = new Info.Builder();
        
        // When
        infoContributor.contribute(builder);
        Info info = builder.build();
        
        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> fireflyInfo = (Map<String, Object>) info.getDetails().get("firefly");
        assertThat(fireflyInfo).isNotNull();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> appInfo = (Map<String, Object>) fireflyInfo.get("application");
        assertThat(appInfo).isNotNull();
        assertThat(appInfo.get("name")).isEqualTo("test-service");
        assertThat(appInfo.get("displayName")).isEqualTo("Test Service");
        assertThat(appInfo.get("version")).isEqualTo("1.0.0");
        assertThat(appInfo.get("description")).isEqualTo("Test description");
        assertThat(appInfo.get("domain")).isEqualTo("test-domain");
        assertThat(appInfo.get("team")).isEqualTo("test-team");
        assertThat(appInfo.get("owners")).isEqualTo(owners);
        assertThat(appInfo.get("apiBasePath")).isEqualTo("/api/v1/test");
        assertThat(appInfo.get("usesServices")).isEqualTo(services);
        assertThat(appInfo.get("capabilities")).isEqualTo(capabilities);
        assertThat(appInfo.get("documentationUrl")).isEqualTo("https://docs.test.com");
        assertThat(appInfo.get("repositoryUrl")).isEqualTo("https://github.com/test/repo");
        assertThat(appInfo.get("deprecated")).isEqualTo(false);
        assertThat(appInfo.get("environment")).isEqualTo("prod");
        assertThat(appInfo.get("buildInfo")).isEqualTo(buildInfo);
    }
    
    @Test
    @DisplayName("Should contribute minimal metadata when optional fields are missing")
    void shouldContributeMinimalMetadata() {
        // Given
        AppMetadata metadata = AppMetadata.builder()
                .name("minimal-service")
                .version("0.1.0")
                .description("Minimal description")
                .domain("minimal-domain")
                .team("minimal-team")
                .owners(new HashSet<>())
                .deprecated(false)
                .environment("dev")
                .startupTime(Instant.now())
                .customProperties(new HashMap<>())
                .build();
        
        infoContributor = new FireflyApplicationInfoContributor(metadata);
        Info.Builder builder = new Info.Builder();
        
        // When
        infoContributor.contribute(builder);
        Info info = builder.build();
        
        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> fireflyInfo = (Map<String, Object>) info.getDetails().get("firefly");
        assertThat(fireflyInfo).isNotNull();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> appInfo = (Map<String, Object>) fireflyInfo.get("application");
        assertThat(appInfo).isNotNull();
        assertThat(appInfo.get("name")).isEqualTo("minimal-service");
        assertThat(appInfo.get("version")).isEqualTo("0.1.0");
        assertThat(appInfo.get("domain")).isEqualTo("minimal-domain");
        assertThat(appInfo.get("team")).isEqualTo("minimal-team");
        assertThat(appInfo.get("deprecated")).isEqualTo(false);
        assertThat(appInfo.get("environment")).isEqualTo("dev");
        
        // Optional fields should not be present
        assertThat(appInfo).doesNotContainKey("apiBasePath");
        assertThat(appInfo).doesNotContainKey("documentationUrl");
        assertThat(appInfo).doesNotContainKey("repositoryUrl");
    }
    
    @Test
    @DisplayName("Should use display name when provided")
    void shouldUseDisplayName() {
        // Given
        AppMetadata metadata = AppMetadata.builder()
                .name("service-name")
                .displayName("Custom Display Name")
                .version("1.0.0")
                .description("Test")
                .domain("domain")
                .team("team")
                .owners(new HashSet<>())
                .deprecated(false)
                .environment("dev")
                .startupTime(Instant.now())
                .customProperties(new HashMap<>())
                .build();
        
        infoContributor = new FireflyApplicationInfoContributor(metadata);
        Info.Builder builder = new Info.Builder();
        
        // When
        infoContributor.contribute(builder);
        Info info = builder.build();
        
        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> fireflyInfo = (Map<String, Object>) info.getDetails().get("firefly");
        @SuppressWarnings("unchecked")
        Map<String, Object> appInfo = (Map<String, Object>) fireflyInfo.get("application");
        assertThat(appInfo.get("displayName")).isEqualTo("Custom Display Name");
    }
    
    @Test
    @DisplayName("Should include deprecation message when deprecated")
    void shouldIncludeDeprecationMessage() {
        // Given
        AppMetadata metadata = AppMetadata.builder()
                .name("deprecated-service")
                .version("1.0.0")
                .description("Test")
                .domain("domain")
                .team("team")
                .owners(new HashSet<>())
                .deprecated(true)
                .deprecationMessage("This service is deprecated. Use new-service instead.")
                .environment("dev")
                .startupTime(Instant.now())
                .customProperties(new HashMap<>())
                .build();
        
        infoContributor = new FireflyApplicationInfoContributor(metadata);
        Info.Builder builder = new Info.Builder();
        
        // When
        infoContributor.contribute(builder);
        Info info = builder.build();
        
        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> fireflyInfo = (Map<String, Object>) info.getDetails().get("firefly");
        @SuppressWarnings("unchecked")
        Map<String, Object> appInfo = (Map<String, Object>) fireflyInfo.get("application");
        assertThat(appInfo.get("deprecated")).isEqualTo(true);
        assertThat(appInfo.get("deprecationMessage"))
                .isEqualTo("This service is deprecated. Use new-service instead.");
    }
    
    @Test
    @DisplayName("Should not include empty collections")
    void shouldNotIncludeEmptyCollections() {
        // Given
        AppMetadata metadata = AppMetadata.builder()
                .name("service")
                .version("1.0.0")
                .description("Test")
                .domain("domain")
                .team("team")
                .owners(new HashSet<>())
                .usesServices(new HashSet<>())
                .capabilities(new HashSet<>())
                .deprecated(false)
                .environment("dev")
                .startupTime(Instant.now())
                .buildInfo(new HashMap<>())
                .customProperties(new HashMap<>())
                .build();
        
        infoContributor = new FireflyApplicationInfoContributor(metadata);
        Info.Builder builder = new Info.Builder();
        
        // When
        infoContributor.contribute(builder);
        Info info = builder.build();
        
        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> fireflyInfo = (Map<String, Object>) info.getDetails().get("firefly");
        @SuppressWarnings("unchecked")
        Map<String, Object> appInfo = (Map<String, Object>) fireflyInfo.get("application");
        
        // Empty collections should not be included
        assertThat(appInfo).doesNotContainKey("owners");
        assertThat(appInfo).doesNotContainKey("usesServices");
        assertThat(appInfo).doesNotContainKey("capabilities");
        assertThat(appInfo).doesNotContainKey("buildInfo");
    }
    
    @Test
    @DisplayName("Should include startup time")
    void shouldIncludeStartupTime() {
        // Given
        Instant startupTime = Instant.parse("2025-01-15T10:30:00Z");
        AppMetadata metadata = AppMetadata.builder()
                .name("service")
                .version("1.0.0")
                .description("Test")
                .domain("domain")
                .team("team")
                .owners(new HashSet<>())
                .deprecated(false)
                .environment("prod")
                .startupTime(startupTime)
                .customProperties(new HashMap<>())
                .build();
        
        infoContributor = new FireflyApplicationInfoContributor(metadata);
        Info.Builder builder = new Info.Builder();
        
        // When
        infoContributor.contribute(builder);
        Info info = builder.build();
        
        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> fireflyInfo = (Map<String, Object>) info.getDetails().get("firefly");
        @SuppressWarnings("unchecked")
        Map<String, Object> appInfo = (Map<String, Object>) fireflyInfo.get("application");
        assertThat(appInfo.get("startupTime")).isEqualTo(startupTime);
    }
}
