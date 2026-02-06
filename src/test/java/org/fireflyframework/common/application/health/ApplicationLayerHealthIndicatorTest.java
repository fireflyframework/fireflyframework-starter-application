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

package org.fireflyframework.application.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApplicationLayerHealthIndicator}.
 */
@DisplayName("ApplicationLayerHealthIndicator Tests")
class ApplicationLayerHealthIndicatorTest {
    
    private ApplicationLayerHealthIndicator healthIndicator;
    
    @BeforeEach
    void setUp() {
        healthIndicator = new ApplicationLayerHealthIndicator();
    }
    
    @Test
    @DisplayName("Should return UP status when components are healthy")
    void shouldReturnUpStatusWhenHealthy() {
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("layer", "application");
        assertThat(health.getDetails()).containsEntry("library", "lib-common-application");
        assertThat(health.getDetails()).containsEntry("contextResolvers", "available");
        assertThat(health.getDetails()).containsEntry("securityServices", "available");
        assertThat(health.getDetails()).containsEntry("configResolvers", "available");
    }
    
    @Test
    @DisplayName("Should include correct layer information in health details")
    void shouldIncludeLayerInformation() {
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getDetails())
                .containsEntry("layer", "application")
                .containsEntry("library", "lib-common-application");
    }
    
    @Test
    @DisplayName("Should include component availability in health details")
    void shouldIncludeComponentAvailability() {
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getDetails())
                .containsEntry("contextResolvers", "available")
                .containsEntry("securityServices", "available")
                .containsEntry("configResolvers", "available");
    }
    
    @Test
    @DisplayName("Should have non-null health status")
    void shouldHaveNonNullHealthStatus() {
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isNotNull();
    }
    
    @Test
    @DisplayName("Should return consistent health status across multiple calls")
    void shouldReturnConsistentHealthStatus() {
        // When
        Health health1 = healthIndicator.health();
        Health health2 = healthIndicator.health();
        
        // Then
        assertThat(health1.getStatus()).isEqualTo(health2.getStatus());
        assertThat(health1.getDetails()).containsAllEntriesOf(health2.getDetails());
    }
}
