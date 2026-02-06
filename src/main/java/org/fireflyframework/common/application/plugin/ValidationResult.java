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

import java.util.Collections;
import java.util.List;

/**
 * Result of input validation before process execution.
 * 
 * <p>ValidationResult contains information about whether the inputs
 * are valid and, if not, detailed error messages for each invalid field.</p>
 * 
 * <h3>Usage Examples</h3>
 * <pre>
 * // Valid result
 * ValidationResult.valid();
 * 
 * // Invalid with errors
 * ValidationResult.invalid()
 *     .error("amount", "Amount must be positive")
 *     .error("currency", "Currency code is not supported")
 *     .build();
 * 
 * // Single error
 * ValidationResult.error("customerId", "Customer not found");
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
public class ValidationResult {
    
    /**
     * Whether the validation passed.
     */
    boolean valid;
    
    /**
     * List of validation errors if validation failed.
     */
    @Singular
    List<ValidationError> errors;
    
    /**
     * Creates a valid result with no errors.
     * 
     * @return a valid ValidationResult
     */
    public static ValidationResult valid() {
        return ValidationResult.builder()
                .valid(true)
                .build();
    }
    
    /**
     * Creates an invalid result builder for adding errors.
     * 
     * @return a builder for invalid result
     */
    public static ValidationResultBuilder invalid() {
        return ValidationResult.builder()
                .valid(false);
    }
    
    /**
     * Creates an invalid result with a single field error.
     * 
     * @param field the field name
     * @param message the error message
     * @return an invalid ValidationResult
     */
    public static ValidationResult error(String field, String message) {
        return ValidationResult.builder()
                .valid(false)
                .error(ValidationError.of(field, message))
                .build();
    }
    
    /**
     * Creates an invalid result with a single error code.
     * 
     * @param code the error code
     * @param message the error message
     * @return an invalid ValidationResult
     */
    public static ValidationResult errorWithCode(String code, String message) {
        return ValidationResult.builder()
                .valid(false)
                .error(ValidationError.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }
    
    /**
     * Creates an invalid result from a list of errors.
     * 
     * @param errors the validation errors
     * @return an invalid ValidationResult
     */
    public static ValidationResult fromErrors(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return valid();
        }
        return ValidationResult.builder()
                .valid(false)
                .errors(errors)
                .build();
    }
    
    /**
     * Checks if there are any errors.
     * 
     * @return true if errors exist
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    /**
     * Gets error messages as a list of strings.
     * 
     * @return list of error messages
     */
    public List<String> getErrorMessages() {
        if (errors == null) {
            return Collections.emptyList();
        }
        return errors.stream()
                .map(ValidationError::getMessage)
                .toList();
    }
    
    /**
     * Gets all errors for a specific field.
     * 
     * @param field the field name
     * @return list of errors for the field
     */
    public List<ValidationError> getErrorsForField(String field) {
        if (errors == null) {
            return Collections.emptyList();
        }
        return errors.stream()
                .filter(e -> field.equals(e.getField()))
                .toList();
    }
    
    /**
     * Checks if there is at least one error for a specific field.
     * 
     * @param field the field name
     * @return true if there is an error for the field
     */
    public boolean hasFieldError(String field) {
        if (errors == null) {
            return false;
        }
        return errors.stream()
                .anyMatch(e -> field.equals(e.getField()));
    }
    
    /**
     * Merges this result with another, combining errors.
     * 
     * @param other the other result to merge
     * @return merged result
     */
    public ValidationResult merge(ValidationResult other) {
        if (other == null || other.isValid()) {
            return this;
        }
        if (this.isValid()) {
            return other;
        }
        return ValidationResult.builder()
                .valid(false)
                .errors(this.errors)
                .errors(other.errors)
                .build();
    }
    
    /**
     * A validation error for a specific field or general validation.
     */
    @Value
    @Builder
    public static class ValidationError {
        
        /**
         * The field that failed validation (may be null for general errors).
         */
        String field;
        
        /**
         * The error code for programmatic handling.
         */
        String code;
        
        /**
         * Human-readable error message.
         */
        String message;
        
        /**
         * The rejected value (for reference).
         */
        Object rejectedValue;
        
        /**
         * Creates a simple field error.
         * 
         * @param field the field name
         * @param message the error message
         * @return a ValidationError
         */
        public static ValidationError of(String field, String message) {
            return ValidationError.builder()
                    .field(field)
                    .message(message)
                    .build();
        }
        
        /**
         * Creates an error with code.
         * 
         * @param field the field name
         * @param code the error code
         * @param message the error message
         * @return a ValidationError
         */
        public static ValidationError of(String field, String code, String message) {
            return ValidationError.builder()
                    .field(field)
                    .code(code)
                    .message(message)
                    .build();
        }
    }
}
