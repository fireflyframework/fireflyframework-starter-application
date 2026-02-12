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

package org.fireflyframework.application.plugin.metrics;

import org.fireflyframework.application.plugin.ProcessResult;
import org.fireflyframework.application.plugin.config.PluginProperties;
import org.fireflyframework.observability.metrics.FireflyMetricsSupport;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for recording plugin execution metrics using Micrometer.
 *
 * <p>This service integrates with the Firefly observability framework to provide
 * comprehensive metrics for plugin system monitoring:</p>
 *
 * <h3>Available Metrics</h3>
 * <ul>
 *   <li>{@code firefly.plugin.executions.total} - Counter of total executions</li>
 *   <li>{@code firefly.plugin.execution.duration} - Timer of execution durations</li>
 *   <li>{@code firefly.plugin.active.count} - Gauge of currently executing plugins</li>
 *   <li>{@code firefly.plugin.registered.count} - Gauge of registered plugins</li>
 *   <li>{@code firefly.plugin.errors} - Counter of errors by type</li>
 * </ul>
 *
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
@Component
@ConditionalOnClass(MeterRegistry.class)
public class PluginMetricsService extends FireflyMetricsSupport {

    private final PluginProperties properties;

    private final AtomicInteger registeredPluginCount = new AtomicInteger(0);
    private final AtomicInteger activeExecutions = new AtomicInteger(0);

    @Autowired
    public PluginMetricsService(MeterRegistry meterRegistry, PluginProperties properties) {
        super(meterRegistry, "plugin");
        this.properties = properties;

        gauge("registered.count", registeredPluginCount, AtomicInteger::get);
        gauge("active.count", activeExecutions, AtomicInteger::get);

        log.info("PluginMetricsService initialized");
    }

    /**
     * Records the start of a plugin execution.
     *
     * @param processId the process ID
     * @param executionId the execution ID
     */
    public void recordExecutionStart(String processId, String executionId) {
        if (!isPluginMetricsEnabled()) {
            return;
        }

        activeExecutions.incrementAndGet();
        counter("executions.total").increment();

        if (isDetailedMetricsEnabled()) {
            counter("process.executions", "process", processId, "type", "started").increment();
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
        if (!isPluginMetricsEnabled()) {
            return;
        }

        activeExecutions.decrementAndGet();

        timer("execution.duration", "status", status.name())
                .record(durationMs, TimeUnit.MILLISECONDS);

        if (status == ProcessResult.Status.SUCCESS) {
            counter("executions.success").increment();
        } else {
            counter("executions.failed").increment();
        }

        if (isDetailedMetricsEnabled()) {
            timer("process.duration", "process", processId)
                    .record(durationMs, TimeUnit.MILLISECONDS);
            counter("process.executions", "process", processId, "type", status.name().toLowerCase())
                    .increment();
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
        if (!isPluginMetricsEnabled()) {
            return;
        }

        counter("errors",
                "process", processId,
                "error.code", errorCode != null ? errorCode : "UNKNOWN",
                "error.type", errorType != null ? errorType : "Unknown")
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
        if (!isPluginMetricsEnabled()) {
            return;
        }

        timer("initialization.duration").record(initializationTimeMs, TimeUnit.MILLISECONDS);
        counter("initialization.plugins").increment(pluginCount);
    }

    /**
     * Records a health check result.
     *
     * @param processId the process ID
     * @param healthy whether the health check passed
     * @param durationMs health check duration
     */
    public void recordHealthCheck(String processId, boolean healthy, long durationMs) {
        if (!isPluginMetricsEnabled()) {
            return;
        }

        timer("healthcheck.duration",
                "process", processId,
                "healthy", String.valueOf(healthy))
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    private boolean isPluginMetricsEnabled() {
        return isEnabled() && properties.getMetrics() != null && properties.getMetrics().isEnabled();
    }

    private boolean isDetailedMetricsEnabled() {
        return isPluginMetricsEnabled() && properties.getMetrics().isDetailedPerProcess();
    }
}
