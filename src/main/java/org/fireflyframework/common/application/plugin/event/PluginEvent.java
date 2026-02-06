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

package org.fireflyframework.application.plugin.event;

import org.fireflyframework.application.plugin.ProcessMetadata;
import org.fireflyframework.application.plugin.ProcessResult;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for plugin-related events.
 * 
 * <p>Plugin events are published via Spring's ApplicationEventPublisher and can
 * be listened to for monitoring, auditing, and reactive processing.</p>
 * 
 * <h3>Available Events</h3>
 * <ul>
 *   <li>{@link PluginRegisteredEvent} - Published when a plugin is registered</li>
 *   <li>{@link PluginUnregisteredEvent} - Published when a plugin is unregistered</li>
 *   <li>{@link PluginExecutionStartedEvent} - Published when execution starts</li>
 *   <li>{@link PluginExecutionCompletedEvent} - Published when execution completes</li>
 *   <li>{@link PluginExecutionFailedEvent} - Published when execution fails</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>
 * &#64;Component
 * public class PluginEventListener {
 *     
 *     &#64;EventListener
 *     public void onPluginRegistered(PluginRegisteredEvent event) {
 *         log.info("Plugin registered: {}", event.getProcessId());
 *     }
 *     
 *     &#64;EventListener
 *     public void onPluginExecutionCompleted(PluginExecutionCompletedEvent event) {
 *         metricsService.recordExecution(event.getProcessId(), event.getDurationMs());
 *     }
 * }
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Getter
public abstract class PluginEvent extends ApplicationEvent {
    
    private final String processId;
    private final String processVersion;
    private final Instant eventTime;
    
    protected PluginEvent(Object source, String processId, String processVersion) {
        super(source);
        this.processId = processId;
        this.processVersion = processVersion;
        this.eventTime = Instant.now();
    }
    
    /**
     * Event published when a plugin is registered in the registry.
     */
    @Getter
    public static class PluginRegisteredEvent extends PluginEvent {
        
        private final ProcessMetadata metadata;
        private final String loaderType;
        
        public PluginRegisteredEvent(Object source, String processId, String processVersion,
                                     ProcessMetadata metadata, String loaderType) {
            super(source, processId, processVersion);
            this.metadata = metadata;
            this.loaderType = loaderType;
        }
    }
    
    /**
     * Event published when a plugin is unregistered from the registry.
     */
    @Getter
    public static class PluginUnregisteredEvent extends PluginEvent {
        
        private final String reason;
        
        public PluginUnregisteredEvent(Object source, String processId, String processVersion, String reason) {
            super(source, processId, processVersion);
            this.reason = reason;
        }
        
        public PluginUnregisteredEvent(Object source, String processId, String processVersion) {
            this(source, processId, processVersion, null);
        }
    }
    
    /**
     * Event published when plugin execution starts.
     */
    @Getter
    public static class PluginExecutionStartedEvent extends PluginEvent {
        
        private final String executionId;
        private final String operationId;
        private final UUID tenantId;
        
        public PluginExecutionStartedEvent(Object source, String processId, String processVersion,
                                           String executionId, String operationId, UUID tenantId) {
            super(source, processId, processVersion);
            this.executionId = executionId;
            this.operationId = operationId;
            this.tenantId = tenantId;
        }
    }
    
    /**
     * Event published when plugin execution completes successfully.
     */
    @Getter
    public static class PluginExecutionCompletedEvent extends PluginEvent {
        
        private final String executionId;
        private final String operationId;
        private final UUID tenantId;
        private final long durationMs;
        private final ProcessResult.Status status;
        
        public PluginExecutionCompletedEvent(Object source, String processId, String processVersion,
                                              String executionId, String operationId, UUID tenantId,
                                              long durationMs, ProcessResult.Status status) {
            super(source, processId, processVersion);
            this.executionId = executionId;
            this.operationId = operationId;
            this.tenantId = tenantId;
            this.durationMs = durationMs;
            this.status = status;
        }
    }
    
    /**
     * Event published when plugin execution fails.
     */
    @Getter
    public static class PluginExecutionFailedEvent extends PluginEvent {
        
        private final String executionId;
        private final String operationId;
        private final UUID tenantId;
        private final long durationMs;
        private final String errorCode;
        private final String errorMessage;
        private final Throwable exception;
        
        public PluginExecutionFailedEvent(Object source, String processId, String processVersion,
                                          String executionId, String operationId, UUID tenantId,
                                          long durationMs, String errorCode, String errorMessage,
                                          Throwable exception) {
            super(source, processId, processVersion);
            this.executionId = executionId;
            this.operationId = operationId;
            this.tenantId = tenantId;
            this.durationMs = durationMs;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.exception = exception;
        }
    }
    
    /**
     * Event published when a plugin fails health check.
     */
    @Getter
    public static class PluginHealthCheckFailedEvent extends PluginEvent {
        
        private final String healthMessage;
        
        public PluginHealthCheckFailedEvent(Object source, String processId, String processVersion,
                                             String healthMessage) {
            super(source, processId, processVersion);
            this.healthMessage = healthMessage;
        }
    }
    
    /**
     * Event published when plugin system initialization completes.
     */
    @Getter
    public static class PluginSystemInitializedEvent extends PluginEvent {
        
        private final int totalPlugins;
        private final int totalVersions;
        private final long initializationTimeMs;
        
        public PluginSystemInitializedEvent(Object source, int totalPlugins, int totalVersions,
                                             long initializationTimeMs) {
            super(source, "system", "1.0.0");
            this.totalPlugins = totalPlugins;
            this.totalVersions = totalVersions;
            this.initializationTimeMs = initializationTimeMs;
        }
    }
}
