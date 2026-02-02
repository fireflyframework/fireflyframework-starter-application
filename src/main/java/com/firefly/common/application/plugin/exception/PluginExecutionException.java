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

package com.firefly.common.application.plugin.exception;

import lombok.Getter;

/**
 * Exception thrown when a plugin execution fails.
 * 
 * <p>This exception preserves context about the failed execution,
 * including the process ID, version, and phase where the failure occurred.</p>
 * 
 * <h3>Execution Phases</h3>
 * <ul>
 *   <li>{@code INITIALIZATION} - Plugin init failed</li>
 *   <li>{@code INPUT_CONVERSION} - Failed to convert input to expected type</li>
 *   <li>{@code VALIDATION} - Input validation failed</li>
 *   <li>{@code EXECUTION} - Business logic execution failed</li>
 *   <li>{@code COMPENSATION} - Compensation/rollback failed</li>
 *   <li>{@code OUTPUT_CONVERSION} - Failed to convert output</li>
 * </ul>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Getter
public class PluginExecutionException extends RuntimeException {
    
    /**
     * Phase of plugin execution where the error occurred.
     */
    public enum Phase {
        INITIALIZATION,
        INPUT_CONVERSION,
        VALIDATION,
        EXECUTION,
        COMPENSATION,
        OUTPUT_CONVERSION,
        UNKNOWN
    }
    
    private final String processId;
    private final String processVersion;
    private final Phase phase;
    private final String executionId;
    
    /**
     * Creates an exception with process context.
     * 
     * @param processId the process ID
     * @param message the error message
     */
    public PluginExecutionException(String processId, String message) {
        super(message);
        this.processId = processId;
        this.processVersion = null;
        this.phase = Phase.UNKNOWN;
        this.executionId = null;
    }
    
    /**
     * Creates an exception with process context and cause.
     * 
     * @param processId the process ID
     * @param message the error message
     * @param cause the underlying cause
     */
    public PluginExecutionException(String processId, String message, Throwable cause) {
        super(message, cause);
        this.processId = processId;
        this.processVersion = null;
        this.phase = Phase.UNKNOWN;
        this.executionId = null;
    }
    
    /**
     * Creates an exception with full context.
     * 
     * @param processId the process ID
     * @param processVersion the process version
     * @param phase the execution phase where failure occurred
     * @param message the error message
     * @param cause the underlying cause
     */
    public PluginExecutionException(
            String processId,
            String processVersion,
            Phase phase,
            String message,
            Throwable cause) {
        super(buildMessage(processId, processVersion, phase, message), cause);
        this.processId = processId;
        this.processVersion = processVersion;
        this.phase = phase;
        this.executionId = null;
    }
    
    /**
     * Creates an exception with full context including execution ID.
     * 
     * @param processId the process ID
     * @param processVersion the process version
     * @param phase the execution phase
     * @param executionId the execution ID for tracing
     * @param message the error message
     * @param cause the underlying cause
     */
    public PluginExecutionException(
            String processId,
            String processVersion,
            Phase phase,
            String executionId,
            String message,
            Throwable cause) {
        super(buildMessage(processId, processVersion, phase, message), cause);
        this.processId = processId;
        this.processVersion = processVersion;
        this.phase = phase;
        this.executionId = executionId;
    }
    
    /**
     * Creates an input conversion exception.
     * 
     * @param processId the process ID
     * @param targetType the target type that failed to convert
     * @param cause the conversion error
     * @return a new PluginExecutionException
     */
    public static PluginExecutionException inputConversionFailed(
            String processId,
            Class<?> targetType,
            Throwable cause) {
        return new PluginExecutionException(
                processId,
                null,
                Phase.INPUT_CONVERSION,
                "Failed to convert input to " + targetType.getName(),
                cause);
    }
    
    /**
     * Creates a validation exception.
     * 
     * @param processId the process ID
     * @param validationMessage the validation error message
     * @return a new PluginExecutionException
     */
    public static PluginExecutionException validationFailed(
            String processId,
            String validationMessage) {
        return new PluginExecutionException(
                processId,
                null,
                Phase.VALIDATION,
                "Validation failed: " + validationMessage,
                null);
    }
    
    /**
     * Creates an execution exception.
     * 
     * @param processId the process ID
     * @param processVersion the process version
     * @param cause the execution error
     * @return a new PluginExecutionException
     */
    public static PluginExecutionException executionFailed(
            String processId,
            String processVersion,
            Throwable cause) {
        return new PluginExecutionException(
                processId,
                processVersion,
                Phase.EXECUTION,
                "Execution failed: " + cause.getMessage(),
                cause);
    }
    
    /**
     * Creates a compensation exception.
     * 
     * @param processId the process ID
     * @param cause the compensation error
     * @return a new PluginExecutionException
     */
    public static PluginExecutionException compensationFailed(
            String processId,
            Throwable cause) {
        return new PluginExecutionException(
                processId,
                null,
                Phase.COMPENSATION,
                "Compensation failed: " + cause.getMessage(),
                cause);
    }
    
    private static String buildMessage(
            String processId,
            String processVersion,
            Phase phase,
            String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("Plugin execution failed");
        
        if (processId != null) {
            sb.append(" [process=").append(processId);
            if (processVersion != null) {
                sb.append(" v").append(processVersion);
            }
            sb.append("]");
        }
        
        if (phase != null && phase != Phase.UNKNOWN) {
            sb.append(" [phase=").append(phase.name()).append("]");
        }
        
        if (message != null) {
            sb.append(": ").append(message);
        }
        
        return sb.toString();
    }
}
