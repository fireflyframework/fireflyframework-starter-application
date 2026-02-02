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

package com.firefly.common.application.plugin;

import com.firefly.common.application.plugin.annotation.FireflyProcess;
import com.firefly.common.application.plugin.exception.PluginExecutionException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Abstract base class for ProcessPlugin implementations that simplifies
 * common patterns and provides sensible defaults.
 * 
 * <p>Extend this class when creating custom process plugins to benefit from:</p>
 * <ul>
 *   <li>Automatic metadata extraction from @FireflyProcess annotation</li>
 *   <li>Type-safe input handling with generic support</li>
 *   <li>Built-in logging and error handling</li>
 *   <li>Simplified execute/validate/compensate pattern</li>
 *   <li>Common utility methods</li>
 * </ul>
 * 
 * <h3>Basic Usage</h3>
 * <pre>
 * &#64;FireflyProcess(
 *     id = "vanilla-account-creation",
 *     name = "Standard Account Creation",
 *     version = "1.0.0"
 * )
 * public class VanillaAccountCreationProcess 
 *         extends AbstractProcessPlugin&lt;AccountCreationRequest, AccountResponse&gt; {
 *     
 *     &#64;Override
 *     protected Mono&lt;AccountResponse&gt; doExecute(
 *             ProcessExecutionContext context,
 *             AccountCreationRequest request) {
 *         // Your business logic here
 *         return accountService.createAccount(request);
 *     }
 * }
 * </pre>
 * 
 * <h3>With Validation</h3>
 * <pre>
 * &#64;Override
 * protected ValidationResult doValidate(
 *         ProcessExecutionContext context,
 *         AccountCreationRequest request) {
 *     if (request.getAmount().compareTo(BigDecimal.ZERO) &lt;= 0) {
 *         return ValidationResult.error("amount", "Amount must be positive");
 *     }
 *     return ValidationResult.valid();
 * }
 * </pre>
 * 
 * @param <I> the input type
 * @param <O> the output type
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractProcessPlugin<I, O> implements ProcessPlugin {
    
    private final ProcessMetadata metadata;
    private final Class<I> inputType;
    private final Class<O> outputType;
    
    /**
     * Creates the plugin with metadata derived from annotations.
     */
    @SuppressWarnings("unchecked")
    protected AbstractProcessPlugin() {
        // Extract type parameters
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) superClass).getActualTypeArguments();
            this.inputType = typeArgs.length > 0 ? (Class<I>) getRawType(typeArgs[0]) : (Class<I>) Map.class;
            this.outputType = typeArgs.length > 1 ? (Class<O>) getRawType(typeArgs[1]) : (Class<O>) Object.class;
        } else {
            this.inputType = (Class<I>) Map.class;
            this.outputType = (Class<O>) Object.class;
        }
        
        // Build metadata from annotation
        this.metadata = buildMetadata();
    }
    
    /**
     * Creates the plugin with explicit metadata.
     * 
     * @param metadata the process metadata
     */
    @SuppressWarnings("unchecked")
    protected AbstractProcessPlugin(ProcessMetadata metadata) {
        this.metadata = metadata;
        
        // Extract type parameters
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType) {
            Type[] typeArgs = ((ParameterizedType) superClass).getActualTypeArguments();
            this.inputType = typeArgs.length > 0 ? (Class<I>) getRawType(typeArgs[0]) : (Class<I>) Map.class;
            this.outputType = typeArgs.length > 1 ? (Class<O>) getRawType(typeArgs[1]) : (Class<O>) Object.class;
        } else {
            this.inputType = (Class<I>) Map.class;
            this.outputType = (Class<O>) Object.class;
        }
    }
    
    private Class<?> getRawType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        return Object.class;
    }
    
    /**
     * Builds metadata from the @FireflyProcess annotation.
     */
    private ProcessMetadata buildMetadata() {
        FireflyProcess annotation = getClass().getAnnotation(FireflyProcess.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "Process plugin must be annotated with @FireflyProcess or provide explicit metadata: " 
                    + getClass().getName());
        }
        
        ProcessMetadata.ProcessMetadataBuilder builder = ProcessMetadata.builder()
                .processId(annotation.id())
                .name(annotation.name())
                .version(annotation.version())
                .description(annotation.description())
                .category(annotation.category().isEmpty() ? null : annotation.category())
                .vanilla(annotation.vanilla())
                .inputType(inputType)
                .outputType(outputType);
        
        // Add capabilities
        for (String capability : annotation.capabilities()) {
            builder.capability(capability);
        }
        
        // Add required permissions
        for (String permission : annotation.requiredPermissions()) {
            builder.requiredPermission(permission);
        }
        
        // Add required roles
        for (String role : annotation.requiredRoles()) {
            builder.requiredRole(role);
        }
        
        // Add tags
        for (String tag : annotation.tags()) {
            builder.tag(tag);
        }
        
        return builder.build();
    }
    
    @Override
    public String getProcessId() {
        return metadata.getProcessId();
    }
    
    @Override
    public String getVersion() {
        return metadata.getVersion();
    }
    
    @Override
    public ProcessMetadata getMetadata() {
        return metadata;
    }
    
    @Override
    public final Mono<ProcessResult> execute(ProcessExecutionContext context) {
        log.debug("Executing process: {} v{}", getProcessId(), getVersion());
        
        try {
            // Convert input to typed object
            I input;
            try {
                input = context.getInput(inputType);
            } catch (Exception e) {
                return handleError(PluginExecutionException.inputConversionFailed(
                        getProcessId(), inputType, e));
            }
            
            // Execute with typed input
            return doExecute(context, input)
                    .map(ProcessResult::success)
                    .doOnSuccess(result -> log.debug("Process {} completed successfully", getProcessId()))
                    .onErrorResume(error -> handleError(
                            PluginExecutionException.executionFailed(getProcessId(), getVersion(), error)));
            
        } catch (Exception e) {
            return handleError(new PluginExecutionException(
                    getProcessId(), getVersion(), PluginExecutionException.Phase.EXECUTION,
                    "Unexpected error during execution", e));
        }
    }
    
    @Override
    public final Mono<ValidationResult> validate(ProcessExecutionContext context) {
        try {
            I input;
            try {
                input = context.getInput(inputType);
            } catch (Exception e) {
                log.warn("Input conversion error during validation in process {}: {}", 
                        getProcessId(), e.getMessage());
                return Mono.just(ValidationResult.errorWithCode(
                        "INPUT_CONVERSION_ERROR", 
                        "Failed to convert input: " + e.getMessage()));
            }
            return Mono.fromSupplier(() -> doValidate(context, input))
                    .onErrorResume(e -> {
                        log.warn("Validation error in process {}: {}", getProcessId(), e.getMessage());
                        return Mono.just(ValidationResult.errorWithCode("VALIDATION_ERROR", e.getMessage()));
                    });
        } catch (Exception e) {
            log.warn("Unexpected validation error in process {}: {}", getProcessId(), e.getMessage());
            return Mono.just(ValidationResult.errorWithCode("VALIDATION_ERROR", e.getMessage()));
        }
    }
    
    @Override
    public final Mono<ProcessResult> compensate(ProcessExecutionContext context) {
        log.info("Compensating process: {}", getProcessId());
        
        try {
            I input;
            try {
                input = context.getInput(inputType);
            } catch (Exception e) {
                return handleError(PluginExecutionException.inputConversionFailed(
                        getProcessId(), inputType, e));
            }
            
            return doCompensate(context, input)
                    .map(result -> ProcessResult.success(Map.of("compensated", true, "result", result)))
                    .switchIfEmpty(Mono.just(ProcessResult.success(Map.of("compensated", true))))
                    .doOnSuccess(r -> log.info("Process {} compensated successfully", getProcessId()))
                    .onErrorResume(error -> handleError(
                            PluginExecutionException.compensationFailed(getProcessId(), error)));
        } catch (Exception e) {
            return handleError(new PluginExecutionException(
                    getProcessId(), getVersion(), PluginExecutionException.Phase.COMPENSATION,
                    "Unexpected error during compensation", e));
        }
    }
    
    /**
     * Implement this method with your business logic.
     * 
     * <p>The input is already converted to the typed object. Return the
     * output object directly - it will be wrapped in a ProcessResult.</p>
     * 
     * @param context the execution context
     * @param input the typed input object
     * @return a Mono emitting the output
     */
    protected abstract Mono<O> doExecute(ProcessExecutionContext context, I input);
    
    /**
     * Override this method to add validation logic.
     * 
     * <p>Default implementation returns valid. Validation runs before
     * execute() and can prevent execution if invalid.</p>
     * 
     * @param context the execution context
     * @param input the typed input object
     * @return the validation result
     */
    protected ValidationResult doValidate(ProcessExecutionContext context, I input) {
        return ValidationResult.valid();
    }
    
    /**
     * Override this method to implement compensation/rollback logic.
     * 
     * <p>Called when a downstream process fails and this process needs
     * to be rolled back. Default implementation returns empty (no output).</p>
     * 
     * @param context the execution context
     * @param input the original typed input
     * @return a Mono that emits compensation result or completes empty
     */
    protected Mono<Object> doCompensate(ProcessExecutionContext context, I input) {
        return Mono.empty();
    }
    
    /**
     * Handles errors during execution.
     * 
     * <p>Override this method to customize error handling.</p>
     * 
     * @param error the error that occurred
     * @return a ProcessResult representing the error
     */
    protected Mono<ProcessResult> handleError(Throwable error) {
        // Unwrap the root cause for proper error classification
        Throwable rootCause = getRootCause(error);
        
        // Check for BusinessException first (either direct or as root cause)
        if (error instanceof BusinessException be) {
            log.warn("Business error in process {}: {} - {}", 
                    getProcessId(), be.getErrorCode(), be.getMessage());
            return Mono.just(ProcessResult.businessError(be.getErrorCode(), be.getMessage()));
        }
        
        if (rootCause instanceof BusinessException be) {
            log.warn("Business error in process {}: {} - {}", 
                    getProcessId(), be.getErrorCode(), be.getMessage());
            return Mono.just(ProcessResult.businessError(be.getErrorCode(), be.getMessage()));
        }
        
        // Handle PluginExecutionException with proper phase information
        if (error instanceof PluginExecutionException pee) {
            log.error("Plugin execution error in process {} [phase={}]: {}", 
                    getProcessId(), pee.getPhase(), error.getMessage(), error);
            return Mono.just(ProcessResult.builder()
                    .status(ProcessResult.Status.TECHNICAL_ERROR)
                    .errorCode(pee.getPhase().name())
                    .errorMessage(rootCause.getMessage())
                    .exception(error)
                    .metadata("phase", pee.getPhase().name())
                    .build());
        }
        
        log.error("Error in process {}: {}", getProcessId(), error.getMessage(), error);
        return Mono.just(ProcessResult.technicalError(error, "PROCESS_ERROR"));
    }
    
    /**
     * Gets the root cause of an exception.
     */
    private Throwable getRootCause(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
    
    /**
     * Gets the expected input type class.
     * 
     * @return the input type
     */
    protected Class<I> getInputType() {
        return inputType;
    }
    
    /**
     * Gets the expected output type class.
     * 
     * @return the output type
     */
    protected Class<O> getOutputType() {
        return outputType;
    }
    
    /**
     * Creates a business error result.
     * 
     * @param code the error code
     * @param message the error message
     * @return a Mono emitting the error result
     */
    protected Mono<O> businessError(String code, String message) {
        return Mono.error(new BusinessException(code, message));
    }
    
    /**
     * Exception for business rule violations.
     */
    public static class BusinessException extends RuntimeException {
        private final String errorCode;
        
        public BusinessException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
}
