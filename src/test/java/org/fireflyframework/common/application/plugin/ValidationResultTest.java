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

import org.fireflyframework.application.plugin.ValidationResult.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationResult Tests")
class ValidationResultTest {
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {
        
        @Test
        @DisplayName("valid() should create a valid result")
        void validShouldCreateValidResult() {
            ValidationResult result = ValidationResult.valid();
            
            assertTrue(result.isValid());
            assertFalse(result.hasErrors());
            assertTrue(result.getErrors().isEmpty());
        }
        
        @Test
        @DisplayName("invalid() should create builder for invalid result")
        void invalidShouldCreateInvalidBuilder() {
            ValidationResult result = ValidationResult.invalid()
                    .error(ValidationError.of("field1", "Error 1"))
                    .error(ValidationError.of("field2", "Error 2"))
                    .build();
            
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
            assertEquals(2, result.getErrors().size());
        }
        
        @Test
        @DisplayName("error() should create single field error")
        void errorShouldCreateSingleFieldError() {
            ValidationResult result = ValidationResult.error("email", "Invalid email format");
            
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
            assertEquals(1, result.getErrors().size());
            assertEquals("email", result.getErrors().get(0).getField());
            assertEquals("Invalid email format", result.getErrors().get(0).getMessage());
        }
        
        @Test
        @DisplayName("errorWithCode() should create error with code")
        void errorWithCodeShouldCreateErrorWithCode() {
            ValidationResult result = ValidationResult.errorWithCode("INVALID_FORMAT", "The format is invalid");
            
            assertFalse(result.isValid());
            assertEquals("INVALID_FORMAT", result.getErrors().get(0).getCode());
            assertEquals("The format is invalid", result.getErrors().get(0).getMessage());
        }
        
        @Test
        @DisplayName("fromErrors() should create result from error list")
        void fromErrorsShouldCreateResultFromList() {
            List<ValidationError> errors = Arrays.asList(
                    ValidationError.of("field1", "Error 1"),
                    ValidationError.of("field2", "Error 2")
            );
            
            ValidationResult result = ValidationResult.fromErrors(errors);
            
            assertFalse(result.isValid());
            assertEquals(2, result.getErrors().size());
        }
        
        @Test
        @DisplayName("fromErrors() with empty list should return valid")
        void fromErrorsWithEmptyListShouldReturnValid() {
            ValidationResult result = ValidationResult.fromErrors(List.of());
            
            assertTrue(result.isValid());
        }
        
        @Test
        @DisplayName("fromErrors() with null should return valid")
        void fromErrorsWithNullShouldReturnValid() {
            ValidationResult result = ValidationResult.fromErrors(null);
            
            assertTrue(result.isValid());
        }
    }
    
    @Nested
    @DisplayName("Error Query Methods")
    class ErrorQueryTests {
        
        @Test
        @DisplayName("hasErrors() should return true when errors exist")
        void hasErrorsShouldReturnTrueWhenErrorsExist() {
            ValidationResult result = ValidationResult.error("field", "Error");
            
            assertTrue(result.hasErrors());
        }
        
        @Test
        @DisplayName("hasErrors() should return false when no errors")
        void hasErrorsShouldReturnFalseWhenNoErrors() {
            ValidationResult result = ValidationResult.valid();
            
            assertFalse(result.hasErrors());
        }
        
        @Test
        @DisplayName("hasFieldError() should return true for field with error")
        void hasFieldErrorShouldReturnTrueForFieldWithError() {
            ValidationResult result = ValidationResult.invalid()
                    .error(ValidationError.of("email", "Invalid"))
                    .error(ValidationError.of("name", "Required"))
                    .build();
            
            assertTrue(result.hasFieldError("email"));
            assertTrue(result.hasFieldError("name"));
        }
        
        @Test
        @DisplayName("hasFieldError() should return false for field without error")
        void hasFieldErrorShouldReturnFalseForFieldWithoutError() {
            ValidationResult result = ValidationResult.error("email", "Invalid");
            
            assertFalse(result.hasFieldError("name"));
        }
        
        @Test
        @DisplayName("hasFieldError() should return false when no errors")
        void hasFieldErrorShouldReturnFalseWhenNoErrors() {
            ValidationResult result = ValidationResult.valid();
            
            assertFalse(result.hasFieldError("anyField"));
        }
        
        @Test
        @DisplayName("getErrorsForField() should return errors for specific field")
        void getErrorsForFieldShouldReturnErrorsForSpecificField() {
            ValidationResult result = ValidationResult.invalid()
                    .error(ValidationError.of("email", "Invalid format"))
                    .error(ValidationError.of("email", "Already exists"))
                    .error(ValidationError.of("name", "Required"))
                    .build();
            
            List<ValidationError> emailErrors = result.getErrorsForField("email");
            
            assertEquals(2, emailErrors.size());
            assertTrue(emailErrors.stream().allMatch(e -> "email".equals(e.getField())));
        }
        
        @Test
        @DisplayName("getErrorsForField() should return empty list for field without errors")
        void getErrorsForFieldShouldReturnEmptyForFieldWithoutErrors() {
            ValidationResult result = ValidationResult.error("email", "Invalid");
            
            List<ValidationError> errors = result.getErrorsForField("name");
            
            assertTrue(errors.isEmpty());
        }
        
        @Test
        @DisplayName("getErrorMessages() should return all error messages")
        void getErrorMessagesShouldReturnAllMessages() {
            ValidationResult result = ValidationResult.invalid()
                    .error(ValidationError.of("field1", "Error 1"))
                    .error(ValidationError.of("field2", "Error 2"))
                    .build();
            
            List<String> messages = result.getErrorMessages();
            
            assertEquals(2, messages.size());
            assertTrue(messages.contains("Error 1"));
            assertTrue(messages.contains("Error 2"));
        }
        
        @Test
        @DisplayName("getErrorMessages() should return empty list when valid")
        void getErrorMessagesShouldReturnEmptyWhenValid() {
            ValidationResult result = ValidationResult.valid();
            
            assertTrue(result.getErrorMessages().isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Merge Tests")
    class MergeTests {
        
        @Test
        @DisplayName("merge() should combine errors from both results")
        void mergeShouldCombineErrors() {
            ValidationResult result1 = ValidationResult.invalid()
                    .error(ValidationError.of("field1", "Error 1"))
                    .build();
            ValidationResult result2 = ValidationResult.invalid()
                    .error(ValidationError.of("field2", "Error 2"))
                    .build();
            
            ValidationResult merged = result1.merge(result2);
            
            assertFalse(merged.isValid());
            assertEquals(2, merged.getErrors().size());
        }
        
        @Test
        @DisplayName("merge() with valid result should return original")
        void mergeWithValidShouldReturnOriginal() {
            ValidationResult invalid = ValidationResult.error("field", "Error");
            ValidationResult valid = ValidationResult.valid();
            
            ValidationResult merged = invalid.merge(valid);
            
            assertEquals(1, merged.getErrors().size());
        }
        
        @Test
        @DisplayName("merge() valid with invalid should return invalid")
        void mergeValidWithInvalidShouldReturnInvalid() {
            ValidationResult valid = ValidationResult.valid();
            ValidationResult invalid = ValidationResult.error("field", "Error");
            
            ValidationResult merged = valid.merge(invalid);
            
            assertFalse(merged.isValid());
            assertEquals(1, merged.getErrors().size());
        }
        
        @Test
        @DisplayName("merge() with null should return original")
        void mergeWithNullShouldReturnOriginal() {
            ValidationResult result = ValidationResult.error("field", "Error");
            
            ValidationResult merged = result.merge(null);
            
            assertEquals(result, merged);
        }
    }
    
    @Nested
    @DisplayName("ValidationError Tests")
    class ValidationErrorTests {
        
        @Test
        @DisplayName("of() should create simple field error")
        void ofShouldCreateSimpleFieldError() {
            ValidationError error = ValidationError.of("email", "Invalid email");
            
            assertEquals("email", error.getField());
            assertEquals("Invalid email", error.getMessage());
            assertNull(error.getCode());
            assertNull(error.getRejectedValue());
        }
        
        @Test
        @DisplayName("of() with code should create error with code")
        void ofWithCodeShouldCreateErrorWithCode() {
            ValidationError error = ValidationError.of("email", "INVALID_FORMAT", "Invalid email format");
            
            assertEquals("email", error.getField());
            assertEquals("INVALID_FORMAT", error.getCode());
            assertEquals("Invalid email format", error.getMessage());
        }
        
        @Test
        @DisplayName("builder should create error with all fields")
        void builderShouldCreateErrorWithAllFields() {
            ValidationError error = ValidationError.builder()
                    .field("amount")
                    .code("MIN_VALUE")
                    .message("Amount must be at least 0")
                    .rejectedValue(-100)
                    .build();
            
            assertEquals("amount", error.getField());
            assertEquals("MIN_VALUE", error.getCode());
            assertEquals("Amount must be at least 0", error.getMessage());
            assertEquals(-100, error.getRejectedValue());
        }
    }
}
