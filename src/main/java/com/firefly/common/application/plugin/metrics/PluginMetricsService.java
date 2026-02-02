/*
 * Copyright 2025 Firefly Software Solutions Inc
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

package com.firefly.common.application.plugin.metrics;

import com.firefly.common.application.plugin.ProcessResult;
import com.firefly.common.application.plugin.config.PluginProperties;
import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for recording plugin execution metrics using Micrometer.
 * 
 * <p>This service integrates with Micrometer to provide comprehensive
 * metrics for plugin system monitoring:</p>
 * 
 * <h3>Available Metrics</h3>
 * <ul>
 *   <li>{@code firefly.plugin.executions} - Counter of executions by process/status</li>
 *   <li>{@code firefly.plugin.execution.duration} - Timer of execution durations</li>
 *   <li>{@code firefly.plugin.active} - Gauge of currently executing plugins</li>
 *   <li>{@code firefly.plugin.registered} - Gauge of registered plugins</li>
 *   <li>{@code firefly.plugin.errors} - Counter of errors by type</li>
 * </ul>
 * 
 * <h3>Configuration</h3>
 * <pre>
 * firefly:
 *   application:
 *     plugin:
 *       metrics:
 *         enabled: true
 *         detailed-per-process: true
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnClass(MeterRegistry.class)
public class PluginMetricsService {
    
    private static final String METRIC_PREFIX = "firefly.plugin";
    
    private final MeterRegistry meterRegistry;
    private final PluginProperties properties;
    
    /**
     * Counter for total executions.
     */
    private final Counter totalExecutions;
    
    /**
     * Counter for successful executions.
     */
    private final Counter successfulExecutions;
    
    /**
     * Counter for failed executions.
     */
    private final Counter failedExecutions;
    
    /**
     * Gauge tracking registered plugin count.
     */
    private final AtomicInteger registeredPluginCount = new AtomicInteger(0);
    
    /**
     * Gauge tracking active executions.
     */
    private final AtomicInteger activeExecutions = new AtomicInteger(0);
    
    /**
     * Cache of per-process timers.
     */
    private final Map<String, Timer> processTimers = new ConcurrentHashMap<>();
    
    /**
     * Cache of per-process counters.
     */
    private final Map<String, Counter> processCounters = new ConcurrentHashMap<>();
    
    @Autowired
    public PluginMetricsService(MeterRegistry meterRegistry, PluginProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
        
        // Initialize global counters
        this.totalExecutions = Counter.builder(METRIC_PREFIX + ".executions.total")
                .description("Total number of plugin executions")
                .register(meterRegistry);
        
        this.successfulExecutions = Counter.builder(METRIC_PREFIX + ".executions.success")
                .description("Number of successful plugin executions")
                .register(meterRegistry);
        
        this.failedExecutions = Counter.builder(METRIC_PREFIX + ".executions.failed")
                .description("Number of failed plugin executions")
                .register(meterRegistry);
        
        // Initialize gauges
        Gauge.builder(METRIC_PREFIX + ".registered.count", registeredPluginCount, AtomicInteger::get)
                .description("Number of registered plugins")
                .register(meterRegistry);
        
        Gauge.builder(METRIC_PREFIX + ".active.count", activeExecutions, AtomicInteger::get)
                .description("Number of currently active plugin executions")
                .register(meterRegistry);
        
        log.info("PluginMetricsService initialized");
    }
    
    /**
     * Records the start of a plugin execution.
     * 
     * @param processId the process ID
     * @param executionId the execution ID
     */
    public void recordExecutionStart(String processId, String executionId) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        activeExecutions.incrementAndGet();
        totalExecutions.increment();
        
        if (isDetailedMetricsEnabled()) {
            getOrCreateCounter(processId, "started").increment();
        }
    }
    
    /**
     * Records the completion of a plugin execution.
     * 
     * @param processId the process ID
     * @param executionId the execution ID
     * @param durationMs execution duration in milliseconds
     * @param status the result status
     */
    public void recordExecutionComplete(String processId, String executionId, 
                                        long durationMs, ProcessResult.Status status) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        activeExecutions.decrementAndGet();
        
        // Record duration
        Timer globalTimer = Timer.builder(METRIC_PREFIX + ".execution.duration")
                .description("Plugin execution duration")
                .tag("status", status.name())
                .register(meterRegistry);
        globalTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        // Record status
        if (status == ProcessResult.Status.SUCCESS) {
            successfulExecutions.increment();
        } else {
            failedExecutions.increment();
        }
        
        if (isDetailedMetricsEnabled()) {
            // Per-process timer
            Timer processTimer = getOrCreateTimer(processId);
            processTimer.record(durationMs, TimeUnit.MILLISECONDS);
            
            // Per-process counter by status
            getOrCreateCounter(processId, status.name().toLowerCase()).increment();
        }
    }
    
    /**
     * Records a plugin execution error.
     * 
     * @param processId the process ID
     * @param errorCode the error code
     * @param errorType the type of error (e.g., exception class name)
     */
    public void recordError(String processId, String errorCode, String errorType) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        Counter.builder(METRIC_PREFIX + ".errors")
                .description("Plugin execution errors")
                .tag("process", processId)
                .tag("errorCode", errorCode != null ? errorCode : "UNKNOWN")
                .tag("errorType", errorType != null ? errorType : "Unknown")
                .register(meterRegistry)
                .increment();
    }
    
    /**
     * Updates the registered plugin count.
     * 
     * @param count the new count
     */
    public void setRegisteredPluginCount(int count) {
        registeredPluginCount.set(count);
    }
    
    /**
     * Increments the registered plugin count.
     */
    public void incrementRegisteredPluginCount() {
        registeredPluginCount.incrementAndGet();
    }
    
    /**
     * Decrements the registered plugin count.
     */
    public void decrementRegisteredPluginCount() {
        registeredPluginCount.decrementAndGet();
    }
    
    /**
     * Records plugin initialization time.
     * 
     * @param initializationTimeMs initialization time in milliseconds
     * @param pluginCount number of plugins loaded
     */
    public void recordInitialization(long initializationTimeMs, int pluginCount) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        Timer.builder(METRIC_PREFIX + ".initialization.duration")
                .description("Plugin system initialization duration")
                .register(meterRegistry)
                .record(initializationTimeMs, TimeUnit.MILLISECONDS);
        
        Counter.builder(METRIC_PREFIX + ".initialization.plugins")
                .description("Plugins loaded during initialization")
                .register(meterRegistry)
                .increment(pluginCount);
    }
    
    /**
     * Records a health check result.
     * 
     * @param processId the process ID
     * @param healthy whether the health check passed
     * @param durationMs health check duration
     */
    public void recordHealthCheck(String processId, boolean healthy, long durationMs) {
        if (!isMetricsEnabled()) {
            return;
        }
        
        Timer.builder(METRIC_PREFIX + ".healthcheck.duration")
                .description("Plugin health check duration")
                .tag("process", processId)
                .tag("healthy", String.valueOf(healthy))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Gets or creates a timer for a specific process.
     */
    private Timer getOrCreateTimer(String processId) {
        return processTimers.computeIfAbsent(processId, id ->
                Timer.builder(METRIC_PREFIX + ".process.duration")
                        .description("Execution duration for process")
                        .tag("process", id)
                        .register(meterRegistry)
        );
    }
    
    /**
     * Gets or creates a counter for a specific process and type.
     */
    private Counter getOrCreateCounter(String processId, String type) {
        String key = processId + ":" + type;
        return processCounters.computeIfAbsent(key, k ->
                Counter.builder(METRIC_PREFIX + ".process.executions")
                        .description("Executions for process")
                        .tag("process", processId)
                        .tag("type", type)
                        .register(meterRegistry)
        );
    }
    
    private boolean isMetricsEnabled() {
        return properties.getMetrics() != null && properties.getMetrics().isEnabled();
    }
    
    private boolean isDetailedMetricsEnabled() {
        return isMetricsEnabled() && properties.getMetrics().isDetailedPerProcess();
    }
}
