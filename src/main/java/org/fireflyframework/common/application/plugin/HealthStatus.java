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

package org.fireflyframework.application.plugin;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * Represents the health status of a process plugin.
 * 
 * <p>This class is used by plugins to report their health status during
 * health checks. The status is aggregated by PluginHealthIndicator for
 * Spring Boot Actuator integration.</p>
 * 
 * <h3>Status Values</h3>
 * <ul>
 *   <li>{@code UP} - Plugin is healthy and ready to process requests</li>
 *   <li>{@code DOWN} - Plugin is unhealthy and cannot process requests</li>
 *   <li>{@code DEGRADED} - Plugin is operational but with reduced functionality</li>
 *   <li>{@code UNKNOWN} - Health status cannot be determined</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>
 * &#64;Override
 * public Mono&lt;HealthStatus&gt; healthCheck() {
 *     return externalService.ping()
 *         .map(response -> HealthStatus.up()
 *             .detail("responseTime", response.getLatency())
 *             .build())
 *         .onErrorReturn(HealthStatus.down("External service unavailable"));
 * }
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
public class HealthStatus {
    
    /**
     * Health status enum.
     */
    public enum Status {
        UP,
        DOWN,
        DEGRADED,
        UNKNOWN
    }
    
    /**
     * The health status.
     */
    Status status;
    
    /**
     * Optional message providing more details about the status.
     */
    String message;
    
    /**
     * Additional details about the health status.
     */
    @Singular
    Map<String, Object> details;
    
    /**
     * Timestamp when the health check was performed.
     */
    @Builder.Default
    long timestamp = System.currentTimeMillis();
    
    /**
     * Creates a healthy status.
     * 
     * @return a HealthStatus with UP status
     */
    public static HealthStatus up() {
        return HealthStatus.builder()
                .status(Status.UP)
                .build();
    }
    
    /**
     * Creates a healthy status with a message.
     * 
     * @param message the status message
     * @return a HealthStatus with UP status
     */
    public static HealthStatus up(String message) {
        return HealthStatus.builder()
                .status(Status.UP)
                .message(message)
                .build();
    }
    
    /**
     * Creates an unhealthy status.
     * 
     * @return a HealthStatus with DOWN status
     */
    public static HealthStatus down() {
        return HealthStatus.builder()
                .status(Status.DOWN)
                .build();
    }
    
    /**
     * Creates an unhealthy status with a message.
     * 
     * @param message the status message
     * @return a HealthStatus with DOWN status
     */
    public static HealthStatus down(String message) {
        return HealthStatus.builder()
                .status(Status.DOWN)
                .message(message)
                .build();
    }
    
    /**
     * Creates an unhealthy status from an exception.
     * 
     * @param error the error that caused the unhealthy status
     * @return a HealthStatus with DOWN status
     */
    public static HealthStatus down(Throwable error) {
        return HealthStatus.builder()
                .status(Status.DOWN)
                .message(error.getMessage())
                .detail("errorType", error.getClass().getName())
                .build();
    }
    
    /**
     * Creates a degraded status.
     * 
     * @param message the status message
     * @return a HealthStatus with DEGRADED status
     */
    public static HealthStatus degraded(String message) {
        return HealthStatus.builder()
                .status(Status.DEGRADED)
                .message(message)
                .build();
    }
    
    /**
     * Creates an unknown status.
     * 
     * @return a HealthStatus with UNKNOWN status
     */
    public static HealthStatus unknown() {
        return HealthStatus.builder()
                .status(Status.UNKNOWN)
                .build();
    }
    
    /**
     * Creates a builder starting with UP status.
     * 
     * @return a builder with UP status
     */
    public static HealthStatusBuilder healthy() {
        return HealthStatus.builder().status(Status.UP);
    }
    
    /**
     * Creates a builder starting with DOWN status.
     * 
     * @return a builder with DOWN status
     */
    public static HealthStatusBuilder unhealthy() {
        return HealthStatus.builder().status(Status.DOWN);
    }
    
    /**
     * Checks if the status indicates the plugin is healthy.
     * 
     * @return true if status is UP
     */
    public boolean isHealthy() {
        return status == Status.UP;
    }
    
    /**
     * Checks if the status indicates the plugin is operational.
     * 
     * @return true if status is UP or DEGRADED
     */
    public boolean isOperational() {
        return status == Status.UP || status == Status.DEGRADED;
    }
}
