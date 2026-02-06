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

package org.fireflyframework.application.plugin.health;

import org.fireflyframework.application.plugin.HealthStatus;
import org.fireflyframework.application.plugin.ProcessPlugin;
import org.fireflyframework.application.plugin.ProcessPluginRegistry;
import org.fireflyframework.application.plugin.config.PluginProperties;
import org.fireflyframework.application.plugin.event.PluginEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring Boot Actuator health indicator for the plugin system.
 * 
 * <p>This health indicator checks the overall health of the plugin system
 * and optionally performs health checks on individual plugins.</p>
 * 
 * <h3>Health Check Components</h3>
 * <ul>
 *   <li>Plugin registry status (are plugins registered?)</li>
 *   <li>Individual plugin health checks (optional)</li>
 *   <li>Plugin loader status</li>
 * </ul>
 * 
 * <h3>Configuration</h3>
 * <pre>
 * firefly:
 *   application:
 *     plugin:
 *       health:
 *         enabled: true
 *         check-individual-plugins: true
 *         timeout: PT5S
 * </pre>
 * 
 * <h3>Actuator Output</h3>
 * <pre>
 * {
 *   "status": "UP",
 *   "details": {
 *     "registeredPlugins": 5,
 *     "totalVersions": 7,
 *     "healthyPlugins": 5,
 *     "unhealthyPlugins": 0,
 *     "plugins": {
 *       "vanilla-account-creation": "UP",
 *       "vanilla-transfer": "UP"
 *     }
 *   }
 * }
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnClass(ReactiveHealthIndicator.class)
public class PluginHealthIndicator implements ReactiveHealthIndicator {
    
    private final ProcessPluginRegistry registry;
    private final PluginProperties properties;
    private final PluginEventPublisher eventPublisher;
    
    @Override
    public Mono<Health> health() {
        if (!isHealthEnabled()) {
            return Mono.just(Health.up()
                    .withDetail("status", "Health checks disabled")
                    .build());
        }
        
        return checkPluginSystemHealth()
                .timeout(getHealthCheckTimeout())
                .onErrorResume(error -> {
                    log.warn("Plugin health check failed: {}", error.getMessage());
                    return Mono.just(Health.down()
                            .withDetail("error", error.getMessage())
                            .build());
                });
    }
    
    /**
     * Performs the plugin system health check.
     */
    private Mono<Health> checkPluginSystemHealth() {
        int registeredCount = registry.size();
        int totalVersions = registry.totalVersionCount();
        
        if (registeredCount == 0) {
            if (properties.getRegistry().isFailOnEmpty()) {
                return Mono.just(Health.down()
                        .withDetail("registeredPlugins", 0)
                        .withDetail("error", "No plugins registered")
                        .build());
            } else {
                return Mono.just(Health.up()
                        .withDetail("registeredPlugins", 0)
                        .withDetail("message", "No plugins registered (not required)")
                        .build());
            }
        }
        
        if (!isIndividualChecksEnabled()) {
            // Just report counts without individual checks
            return Mono.just(Health.up()
                    .withDetail("registeredPlugins", registeredCount)
                    .withDetail("totalVersions", totalVersions)
                    .build());
        }
        
        // Perform individual plugin health checks
        return checkAllPlugins()
                .collectList()
                .map(results -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("registeredPlugins", registeredCount);
                    details.put("totalVersions", totalVersions);
                    
                    Map<String, String> pluginStatus = new HashMap<>();
                    AtomicInteger healthyCount = new AtomicInteger(0);
                    AtomicInteger unhealthyCount = new AtomicInteger(0);
                    
                    for (PluginHealthResult result : results) {
                        pluginStatus.put(result.processId(), result.status().name());
                        if (result.status() == HealthStatus.Status.UP) {
                            healthyCount.incrementAndGet();
                        } else if (result.status() == HealthStatus.Status.DOWN) {
                            unhealthyCount.incrementAndGet();
                        }
                    }
                    
                    details.put("healthyPlugins", healthyCount.get());
                    details.put("unhealthyPlugins", unhealthyCount.get());
                    details.put("plugins", pluginStatus);
                    
                    Health.Builder builder;
                    if (unhealthyCount.get() > 0) {
                        // If any plugins are unhealthy, report as degraded (not down)
                        builder = Health.status("DEGRADED");
                    } else {
                        builder = Health.up();
                    }
                    
                    details.forEach(builder::withDetail);
                    return builder.build();
                });
    }
    
    /**
     * Checks health of all registered plugins.
     */
    private Flux<PluginHealthResult> checkAllPlugins() {
        return Flux.fromIterable(registry.getAll())
                .flatMap(this::checkPluginHealth)
                .timeout(getHealthCheckTimeout());
    }
    
    /**
     * Checks health of a single plugin.
     */
    private Mono<PluginHealthResult> checkPluginHealth(ProcessPlugin plugin) {
        String processId = plugin.getProcessId();
        long startTime = System.currentTimeMillis();
        
        return plugin.healthCheck()
                .timeout(Duration.ofSeconds(5))  // Per-plugin timeout
                .map(status -> {
                    if (!status.isHealthy()) {
                        eventPublisher.publishHealthCheckFailed(
                                processId, 
                                plugin.getVersion(), 
                                status.getMessage()
                        );
                    }
                    return new PluginHealthResult(processId, status.getStatus(), status.getMessage());
                })
                .onErrorResume(error -> {
                    log.warn("Health check failed for plugin {}: {}", processId, error.getMessage());
                    eventPublisher.publishHealthCheckFailed(processId, plugin.getVersion(), error.getMessage());
                    return Mono.just(new PluginHealthResult(
                            processId, 
                            HealthStatus.Status.DOWN, 
                            "Health check error: " + error.getMessage()
                    ));
                })
                .doOnNext(result -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.debug("Health check for {} completed in {}ms: {}", 
                            processId, duration, result.status());
                });
    }
    
    private boolean isHealthEnabled() {
        return properties.getHealth() != null && properties.getHealth().isEnabled();
    }
    
    private boolean isIndividualChecksEnabled() {
        return isHealthEnabled() && properties.getHealth().isCheckIndividualPlugins();
    }
    
    private Duration getHealthCheckTimeout() {
        if (properties.getHealth() != null && properties.getHealth().getTimeout() != null) {
            return properties.getHealth().getTimeout();
        }
        return Duration.ofSeconds(10);
    }
    
    /**
     * Record for plugin health check results.
     */
    private record PluginHealthResult(String processId, HealthStatus.Status status, String message) {}
}
