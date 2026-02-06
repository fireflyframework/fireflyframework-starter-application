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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fireflyframework.application.plugin.config.PluginObjectMapperConfig;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * Result of a process plugin execution.
 * 
 * <p>ProcessResult encapsulates the outcome of a process execution,
 * including success/failure status, output data, error information,
 * and metadata about the execution.</p>
 * 
 * <h3>Usage Examples</h3>
 * <pre>
 * // Success with output
 * ProcessResult.success(createdAccount);
 * 
 * // Success with additional metadata
 * ProcessResult.builder()
 *     .status(ProcessResult.Status.SUCCESS)
 *     .output(createdAccount)
 *     .metadata("auditId", auditRecord.getId())
 *     .build();
 * 
 * // Failure with error code
 * ProcessResult.failed("INSUFFICIENT_BALANCE", "Account has insufficient funds");
 * 
 * // Business validation failure
 * ProcessResult.businessError("KYC_FAILED", "Customer KYC verification failed");
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
public class ProcessResult {
    
    /**
     * Gets the configured ObjectMapper for output conversion.
     * Uses the centralized PluginObjectMapperConfig for proper configuration.
     * 
     * @return the configured ObjectMapper
     */
    private static ObjectMapper getObjectMapper() {
        return PluginObjectMapperConfig.getInstance();
    }
    
    /**
     * Execution status.
     */
    Status status;
    
    /**
     * The output data from the process.
     * Can be any object type - use {@code getOutput(Class)} for typed access.
     */
    Object output;
    
    /**
     * Error code for failed executions.
     */
    String errorCode;
    
    /**
     * Error message for failed executions.
     */
    String errorMessage;
    
    /**
     * The underlying exception if execution failed due to an error.
     */
    Throwable exception;
    
    /**
     * Additional metadata about the execution.
     */
    @Singular("metadata")
    Map<String, Object> metadata;
    
    /**
     * Time taken to execute the process in milliseconds.
     */
    long executionTimeMs;
    
    /**
     * Execution ID for tracing.
     */
    String executionId;
    
    /**
     * Status of a process result.
     */
    public enum Status {
        /**
         * Process completed successfully.
         */
        SUCCESS,
        
        /**
         * Process failed due to a business rule or validation.
         */
        BUSINESS_ERROR,
        
        /**
         * Process failed due to a technical error.
         */
        TECHNICAL_ERROR,
        
        /**
         * Process requires additional action (e.g., approval).
         */
        PENDING,
        
        /**
         * Process was partially completed.
         */
        PARTIAL
    }
    
    /**
     * Creates a successful result with output.
     * 
     * @param output the process output
     * @return a successful ProcessResult
     */
    public static ProcessResult success(Object output) {
        return ProcessResult.builder()
                .status(Status.SUCCESS)
                .output(output)
                .build();
    }
    
    /**
     * Creates a successful result without output.
     * 
     * @return a successful ProcessResult
     */
    public static ProcessResult success() {
        return ProcessResult.builder()
                .status(Status.SUCCESS)
                .build();
    }
    
    /**
     * Creates a failed result with error code and message.
     * 
     * @param errorCode the error code
     * @param errorMessage the error message
     * @return a failed ProcessResult
     */
    public static ProcessResult failed(String errorCode, String errorMessage) {
        return ProcessResult.builder()
                .status(Status.TECHNICAL_ERROR)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Creates a failed result with error code.
     * 
     * @param errorCode the error code
     * @return a failed ProcessResult
     */
    public static ProcessResult failed(String errorCode) {
        return failed(errorCode, null);
    }
    
    /**
     * Creates a failed result from an exception.
     * 
     * @param exception the exception
     * @return a failed ProcessResult
     */
    public static ProcessResult failed(Throwable exception) {
        return ProcessResult.builder()
                .status(Status.TECHNICAL_ERROR)
                .errorCode(exception.getClass().getSimpleName())
                .errorMessage(exception.getMessage())
                .exception(exception)
                .build();
    }
    
    /**
     * Creates a technical error result with exception and error code.
     * 
     * @param exception the exception
     * @param errorCode the error code
     * @return a technical error ProcessResult
     */
    public static ProcessResult technicalError(Throwable exception, String errorCode) {
        return ProcessResult.builder()
                .status(Status.TECHNICAL_ERROR)
                .errorCode(errorCode)
                .errorMessage(exception.getMessage())
                .exception(exception)
                .build();
    }
    
    /**
     * Creates a business error result.
     * 
     * @param errorCode the error code
     * @param errorMessage the error message
     * @return a business error ProcessResult
     */
    public static ProcessResult businessError(String errorCode, String errorMessage) {
        return ProcessResult.builder()
                .status(Status.BUSINESS_ERROR)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Creates a pending result (requires further action).
     * 
     * @param output partial output or status information
     * @return a pending ProcessResult
     */
    public static ProcessResult pending(Object output) {
        return ProcessResult.builder()
                .status(Status.PENDING)
                .output(output)
                .build();
    }
    
    /**
     * Creates a partial result (partially completed).
     * 
     * @param output partial output
     * @param errorMessage description of what failed
     * @return a partial ProcessResult
     */
    public static ProcessResult partial(Object output, String errorMessage) {
        return ProcessResult.builder()
                .status(Status.PARTIAL)
                .output(output)
                .errorMessage(errorMessage)
                .build();
    }
    
    /**
     * Checks if the result is successful.
     * 
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    /**
     * Checks if the result is a failure.
     * 
     * @return true if status is BUSINESS_ERROR or TECHNICAL_ERROR
     */
    public boolean isFailed() {
        return status == Status.BUSINESS_ERROR || status == Status.TECHNICAL_ERROR;
    }
    
    /**
     * Checks if the result is pending.
     * 
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return status == Status.PENDING;
    }
    
    /**
     * Gets the output as a typed object.
     * 
     * @param type the expected output type
     * @param <T> the type parameter
     * @return the typed output, or null if no output
     * @throws IllegalArgumentException if conversion fails
     */
    @SuppressWarnings("unchecked")
    public <T> T getOutput(Class<T> type) {
        if (output == null) {
            return null;
        }
        if (type.isInstance(output)) {
            return (T) output;
        }
        try {
            return getObjectMapper().convertValue(output, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert output to " + type.getName(), e);
        }
    }
    
    /**
     * Gets a metadata value.
     * 
     * @param key the metadata key
     * @param type the expected type
     * @param <T> the type parameter
     * @return the metadata value, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
    
    /**
     * Creates a new result with execution timing information.
     * 
     * @param startTime execution start time (epoch millis)
     * @param executionId the execution ID
     * @return a new ProcessResult with timing
     */
    public ProcessResult withTiming(long startTime, String executionId) {
        return toBuilder()
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .executionId(executionId)
                .build();
    }
}
