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

package org.fireflyframework.application.integration;

import org.fireflyframework.application.context.AppMetadata;
import org.fireflyframework.application.metadata.ApplicationMetadataProvider;
import org.fireflyframework.application.metadata.FireflyApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ApplicationMetadataProvider}.
 * Tests the scanning and building of AppMetadata from @FireflyApplication annotation.
 */
@SpringBootTest(classes = ApplicationMetadataProviderIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
    "spring.application.name=test-application",
    "spring.application.version=1.0.0-TEST",
    "git.commit=abc123",
    "git.branch=main"
})
@DisplayName("ApplicationMetadataProvider Integration Tests")
class ApplicationMetadataProviderIntegrationTest {
    
    @Autowired
    private AppMetadata appMetadata;
    
    @Test
    @DisplayName("Should create AppMetadata bean from @FireflyApplication annotation")
    void shouldCreateAppMetadataBean() {
        assertThat(appMetadata).isNotNull();
    }
    
    @Test
    @DisplayName("Should extract name from @FireflyApplication")
    void shouldExtractNameFromAnnotation() {
        assertThat(appMetadata.getName()).isEqualTo("test-service");
    }
    
    @Test
    @DisplayName("Should extract display name from @FireflyApplication")
    void shouldExtractDisplayNameFromAnnotation() {
        assertThat(appMetadata.getDisplayName()).isEqualTo("Test Service");
    }
    
    @Test
    @DisplayName("Should extract description from @FireflyApplication")
    void shouldExtractDescriptionFromAnnotation() {
        assertThat(appMetadata.getDescription())
                .isEqualTo("A test service for integration testing");
    }
    
    @Test
    @DisplayName("Should extract domain from @FireflyApplication")
    void shouldExtractDomainFromAnnotation() {
        assertThat(appMetadata.getDomain()).isEqualTo("test");
    }
    
    @Test
    @DisplayName("Should extract team from @FireflyApplication")
    void shouldExtractTeamFromAnnotation() {
        assertThat(appMetadata.getTeam()).isEqualTo("test-team");
    }
    
    @Test
    @DisplayName("Should extract owners from @FireflyApplication")
    void shouldExtractOwnersFromAnnotation() {
        assertThat(appMetadata.getOwners())
                .containsExactlyInAnyOrder("owner1@test.com", "owner2@test.com");
    }
    
    @Test
    @DisplayName("Should extract API base path from @FireflyApplication")
    void shouldExtractApiBasePathFromAnnotation() {
        assertThat(appMetadata.getApiBasePath()).isEqualTo("/api/v1/test");
    }
    
    @Test
    @DisplayName("Should extract used services from @FireflyApplication")
    void shouldExtractUsedServicesFromAnnotation() {
        assertThat(appMetadata.getUsesServices())
                .containsExactlyInAnyOrder("service-1", "service-2");
    }
    
    @Test
    @DisplayName("Should extract capabilities from @FireflyApplication")
    void shouldExtractCapabilitiesFromAnnotation() {
        assertThat(appMetadata.getCapabilities())
                .containsExactlyInAnyOrder("capability-1", "capability-2");
    }
    
    @Test
    @DisplayName("Should get version from spring properties")
    void shouldGetVersionFromSpringProperties() {
        assertThat(appMetadata.getVersion()).isEqualTo("1.0.0-TEST");
    }
    
    @Test
    @DisplayName("Should resolve environment from active profiles")
    void shouldResolveEnvironmentFromProfiles() {
        // Environment is resolved from active profiles or defaults
        assertThat(appMetadata.getEnvironment()).isNotNull();
    }
    
    @Test
    @DisplayName("Should have startup time set")
    void shouldHaveStartupTimeSet() {
        assertThat(appMetadata.getStartupTime()).isNotNull();
    }
    
    @Test
    @DisplayName("Should extract build info from properties")
    void shouldExtractBuildInfoFromProperties() {
        assertThat(appMetadata.getBuildInfo()).isNotEmpty();
        assertThat(appMetadata.getBuildInfo()).containsKey("git.commit");
        assertThat(appMetadata.getBuildInfo()).containsKey("git.branch");
    }
    
    @Test
    @DisplayName("Should use effective display name")
    void shouldUseEffectiveDisplayName() {
        assertThat(appMetadata.getEffectiveDisplayName()).isEqualTo("Test Service");
    }
    
    @Test
    @DisplayName("Should not be deprecated by default")
    void shouldNotBeDeprecatedByDefault() {
        assertThat(appMetadata.isDeprecated()).isFalse();
    }
    
    /**
     * Test configuration with @FireflyApplication annotation.
     */
    @SpringBootConfiguration
    @FireflyApplication(
        name = "test-service",
        displayName = "Test Service",
        description = "A test service for integration testing",
        domain = "test",
        team = "test-team",
        owners = {"owner1@test.com", "owner2@test.com"},
        apiBasePath = "/api/v1/test",
        usesServices = {"service-1", "service-2"},
        capabilities = {"capability-1", "capability-2"}
    )
    @Import(ApplicationMetadataProvider.class)
    static class TestConfig {
    }
}
