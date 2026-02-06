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
import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.plugin.config.PluginObjectMapperConfig;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;
import java.util.UUID;

/**
 * Context for process plugin execution, combining the application execution
 * context with process-specific information and inputs.
 * 
 * <p>ProcessExecutionContext provides everything a process plugin needs to
 * execute its business logic:</p>
 * <ul>
 *   <li>Full application context (security, tenant, party, config)</li>
 *   <li>Process identification and mapping information</li>
 *   <li>Request inputs and headers</li>
 *   <li>Correlation and tracing information</li>
 * </ul>
 * 
 * <h3>Accessing Inputs</h3>
 * <pre>
 * // Type-safe input access
 * AccountCreationRequest request = context.getInput(AccountCreationRequest.class);
 * 
 * // Map-based access for dynamic inputs
 * String customerId = context.getInputValue("customerId", String.class);
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
public class ProcessExecutionContext {
    
    /**
     * Gets the configured ObjectMapper for input conversion.
     * Uses the centralized PluginObjectMapperConfig for proper configuration.
     * 
     * @return the configured ObjectMapper
     */
    private static ObjectMapper getObjectMapper() {
        return PluginObjectMapperConfig.getInstance();
    }
    
    /**
     * The underlying application execution context.
     * Contains security, tenant, party, and config information.
     */
    ApplicationExecutionContext appContext;
    
    /**
     * The process ID being executed.
     */
    String processId;
    
    /**
     * The API operation that triggered this process.
     * Example: "createAccount", "initiateTransfer"
     */
    String operationId;
    
    /**
     * The process mapping that resolved this process.
     * Contains tenant/product/channel specific configuration.
     */
    ProcessMapping processMapping;
    
    /**
     * The request input data as a map.
     * Can be converted to typed objects via {@code getInput()}.
     */
    @Singular
    Map<String, Object> inputs;
    
    /**
     * Additional HTTP headers from the request.
     */
    @Singular
    Map<String, String> headers;
    
    /**
     * Unique correlation ID for tracing.
     * Used for logging and distributed tracing.
     */
    String correlationId;
    
    /**
     * Optional parent execution ID for nested process calls.
     */
    String parentExecutionId;
    
    /**
     * Current execution ID for this process invocation.
     */
    @Builder.Default
    String executionId = UUID.randomUUID().toString();
    
    /**
     * Timestamp when the process execution started (epoch millis).
     */
    @Builder.Default
    long startTime = System.currentTimeMillis();
    
    /**
     * Additional context properties for process-specific data.
     */
    @Singular
    Map<String, Object> properties;
    
    /**
     * Gets the tenant ID from the application context.
     * 
     * @return the tenant ID
     */
    public UUID getTenantId() {
        return appContext != null ? appContext.getTenantId() : null;
    }
    
    /**
     * Gets the party ID from the application context.
     * 
     * @return the party ID
     */
    public UUID getPartyId() {
        return appContext != null ? appContext.getPartyId() : null;
    }
    
    /**
     * Gets the product ID from the application context.
     * 
     * @return the product ID (may be null)
     */
    public UUID getProductId() {
        return appContext != null ? appContext.getProductId() : null;
    }
    
    /**
     * Gets the contract ID from the application context.
     * 
     * @return the contract ID (may be null)
     */
    public UUID getContractId() {
        return appContext != null ? appContext.getContractId() : null;
    }
    
    /**
     * Converts the input map to a typed object.
     * 
     * @param type the target type class
     * @param <T> the target type
     * @return the converted input object
     * @throws IllegalArgumentException if conversion fails
     */
    public <T> T getInput(Class<T> type) {
        if (inputs == null || inputs.isEmpty()) {
            return null;
        }
        try {
            return getObjectMapper().convertValue(inputs, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert input to " + type.getName(), e);
        }
    }
    
    /**
     * Gets a specific input value by key.
     * 
     * @param key the input key
     * @param type the expected type
     * @param <T> the value type
     * @return the value, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getInputValue(String key, Class<T> type) {
        if (inputs == null) {
            return null;
        }
        Object value = inputs.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        return getObjectMapper().convertValue(value, type);
    }
    
    /**
     * Gets an input value with a default fallback.
     * 
     * @param key the input key
     * @param type the expected type
     * @param defaultValue the default if not present
     * @param <T> the value type
     * @return the value or default
     */
    public <T> T getInputValue(String key, Class<T> type, T defaultValue) {
        T value = getInputValue(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Checks if a specific input exists.
     * 
     * @param key the input key
     * @return true if the input exists
     */
    public boolean hasInput(String key) {
        return inputs != null && inputs.containsKey(key);
    }
    
    /**
     * Gets a header value.
     * 
     * @param name the header name
     * @return the header value, or null if not present
     */
    public String getHeader(String name) {
        return headers != null ? headers.get(name) : null;
    }
    
    /**
     * Gets a property value.
     * 
     * @param key the property key
     * @param type the expected type
     * @param <T> the value type
     * @return the property value, or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> type) {
        if (properties == null) {
            return null;
        }
        Object value = properties.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
    
    /**
     * Checks if a feature is enabled for this context.
     * 
     * @param feature the feature flag name
     * @return true if the feature is enabled
     */
    public boolean isFeatureEnabled(String feature) {
        return appContext != null && appContext.isFeatureEnabled(feature);
    }
    
    /**
     * Checks if the context has a specific role.
     * 
     * @param role the role to check
     * @return true if the role is present
     */
    public boolean hasRole(String role) {
        return appContext != null && appContext.hasRole(role);
    }
    
    /**
     * Creates a new context with additional inputs merged.
     * 
     * @param additionalInputs inputs to add
     * @return a new context with merged inputs
     */
    public ProcessExecutionContext withAdditionalInputs(Map<String, Object> additionalInputs) {
        if (additionalInputs == null || additionalInputs.isEmpty()) {
            return this;
        }
        return toBuilder()
                .clearInputs()
                .inputs(this.inputs)
                .inputs(additionalInputs)
                .build();
    }
    
    /**
     * Creates a child context for nested process execution.
     * 
     * @param childProcessId the child process ID
     * @param childOperationId the child operation ID
     * @return a new context for the child process
     */
    public ProcessExecutionContext createChildContext(String childProcessId, String childOperationId) {
        return toBuilder()
                .processId(childProcessId)
                .operationId(childOperationId)
                .parentExecutionId(this.executionId)
                .executionId(UUID.randomUUID().toString())
                .startTime(System.currentTimeMillis())
                .build();
    }
}
