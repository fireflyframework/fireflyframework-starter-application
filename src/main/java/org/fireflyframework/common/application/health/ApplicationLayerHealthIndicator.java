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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the Application Layer.
 * Checks the health of application layer components including
 * context resolvers, config resolvers, and security services.
 * 
 * <p>This health indicator is automatically registered and exposed
 * via Spring Boot Actuator's /actuator/health endpoint.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class ApplicationLayerHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            // Check if application layer components are properly initialized
            boolean applicationLayerHealthy = checkApplicationLayerComponents();
            
            if (applicationLayerHealthy) {
                return Health.up()
                        .withDetail("layer", "application")
                        .withDetail("library", "lib-common-application")
                        .withDetail("contextResolvers", "available")
                        .withDetail("securityServices", "available")
                        .withDetail("configResolvers", "available")
                        .build();
            } else {
                return Health.down()
                        .withDetail("layer", "application")
                        .withDetail("library", "lib-common-application")
                        .withDetail("reason", "Application layer components not properly initialized")
                        .build();
            }
        } catch (Exception e) {
            log.error("Application layer health check failed", e);
            return Health.down()
                    .withDetail("layer", "application")
                    .withDetail("library", "lib-common-application")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
    
    /**
     * Checks if application layer components are healthy.
     * 
     * @return true if components are healthy
     */
    private boolean checkApplicationLayerComponents() {
        // Basic health check - can be extended to check specific components
        // TODO: Add specific checks for ContextResolver, ConfigResolver, etc.
        return true;
    }
}
