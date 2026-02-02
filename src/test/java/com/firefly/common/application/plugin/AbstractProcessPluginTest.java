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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AbstractProcessPlugin Tests")
class AbstractProcessPluginTest {
    
    // Sample request/response DTOs for testing
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestRequest {
        private String name;
        private BigDecimal amount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class TestResponse {
        private String id;
        private String status;
    }
    
    // Test plugin implementation
    @FireflyProcess(
            id = "test-process",
            name = "Test Process",
            version = "1.0.0",
            description = "A test process plugin",
            category = "TEST",
            capabilities = {"capability1", "capability2"},
            requiredPermissions = {"test:read", "test:write"},
            requiredRoles = {"USER"},
            tags = {"test", "sample"},
            vanilla = true
    )
    static class TestProcessPlugin extends AbstractProcessPlugin<TestRequest, TestResponse> {
        
        @Override
        protected Mono<TestResponse> doExecute(ProcessExecutionContext context, TestRequest input) {
            return Mono.just(TestResponse.builder()
                    .id("generated-id")
                    .status("COMPLETED")
                    .build());
        }
        
        @Override
        protected ValidationResult doValidate(ProcessExecutionContext context, TestRequest input) {
            if (input.getName() == null || input.getName().isEmpty()) {
                return ValidationResult.error("name", "Name is required");
            }
            if (input.getAmount() != null && input.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                return ValidationResult.error("amount", "Amount cannot be negative");
            }
            return ValidationResult.valid();
        }
    }
    
    // Plugin that returns business error
    @FireflyProcess(id = "error-process", version = "1.0.0")
    static class ErrorProcessPlugin extends AbstractProcessPlugin<TestRequest, TestResponse> {
        
        @Override
        protected Mono<TestResponse> doExecute(ProcessExecutionContext context, TestRequest input) {
            if ("fail".equals(input.getName())) {
                return businessError("BUSINESS_RULE_VIOLATED", "Business rule failed");
            }
            return Mono.just(TestResponse.builder().status("OK").build());
        }
    }
    
    // Plugin with compensation
    @FireflyProcess(id = "compensating-process", version = "1.0.0")
    static class CompensatingProcessPlugin extends AbstractProcessPlugin<TestRequest, TestResponse> {
        
        @Override
        protected Mono<TestResponse> doExecute(ProcessExecutionContext context, TestRequest input) {
            return Mono.just(TestResponse.builder().id("txn-123").status("PROCESSED").build());
        }
        
        @Override
        protected Mono<Object> doCompensate(ProcessExecutionContext context, TestRequest input) {
            return Mono.just(Map.of("reversed", true, "originalTxnId", "txn-123"));
        }
    }
    
    private ProcessExecutionContext createContext(Map<String, Object> inputs) {
        return ProcessExecutionContext.builder()
                .processId("test")
                .executionId("exec-123")
                .inputs(inputs)
                .build();
    }
    
    @Nested
    @DisplayName("Metadata Extraction Tests")
    class MetadataTests {
        
        @Test
        @DisplayName("Should extract metadata from @FireflyProcess annotation")
        void shouldExtractMetadataFromAnnotation() {
            TestProcessPlugin plugin = new TestProcessPlugin();
            
            assertEquals("test-process", plugin.getProcessId());
            assertEquals("1.0.0", plugin.getVersion());
            
            ProcessMetadata metadata = plugin.getMetadata();
            assertNotNull(metadata);
            assertEquals("Test Process", metadata.getName());
            assertEquals("A test process plugin", metadata.getDescription());
            assertEquals("TEST", metadata.getCategory());
            assertTrue(metadata.isVanilla());
            assertEquals(Set.of("capability1", "capability2"), metadata.getCapabilities());
            assertEquals(Set.of("test:read", "test:write"), metadata.getRequiredPermissions());
            assertEquals(Set.of("USER"), metadata.getRequiredRoles());
            assertEquals(Set.of("test", "sample"), metadata.getTags());
        }
        
        @Test
        @DisplayName("Should extract type information from generic parameters")
        void shouldExtractTypeInformation() {
            TestProcessPlugin plugin = new TestProcessPlugin();
            
            assertEquals(TestRequest.class, plugin.getInputType());
            assertEquals(TestResponse.class, plugin.getOutputType());
        }
    }
    
    @Nested
    @DisplayName("Execution Tests")
    class ExecutionTests {
        
        @Test
        @DisplayName("Should execute process and return result")
        void shouldExecuteProcessSuccessfully() {
            TestProcessPlugin plugin = new TestProcessPlugin();
            
            ProcessExecutionContext context = createContext(Map.of(
                    "name", "Test",
                    "amount", 100.0
            ));
            
            StepVerifier.create(plugin.execute(context))
                    .assertNext(result -> {
                        assertTrue(result.isSuccess());
                        TestResponse output = result.getOutput(TestResponse.class);
                        assertEquals("generated-id", output.getId());
                        assertEquals("COMPLETED", output.getStatus());
                    })
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should handle business errors correctly")
        void shouldHandleBusinessErrors() {
            ErrorProcessPlugin plugin = new ErrorProcessPlugin();
            
            ProcessExecutionContext context = createContext(Map.of("name", "fail"));
            
            StepVerifier.create(plugin.execute(context))
                    .assertNext(result -> {
                        assertFalse(result.isSuccess());
                        assertEquals(ProcessResult.Status.BUSINESS_ERROR, result.getStatus());
                        assertEquals("BUSINESS_RULE_VIOLATED", result.getErrorCode());
                        assertEquals("Business rule failed", result.getErrorMessage());
                    })
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {
        
        @Test
        @DisplayName("Should return valid when input is correct")
        void shouldReturnValidForCorrectInput() {
            TestProcessPlugin plugin = new TestProcessPlugin();
            
            ProcessExecutionContext context = createContext(Map.of(
                    "name", "ValidName",
                    "amount", 100.0
            ));
            
            StepVerifier.create(plugin.validate(context))
                    .assertNext(result -> {
                        assertTrue(result.isValid());
                        assertTrue(result.getErrors().isEmpty());
                    })
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should return errors for invalid input")
        void shouldReturnErrorsForInvalidInput() {
            TestProcessPlugin plugin = new TestProcessPlugin();
            
            ProcessExecutionContext context = createContext(Map.of(
                    "name", "",
                    "amount", -50.0
            ));
            
            StepVerifier.create(plugin.validate(context))
                    .assertNext(result -> {
                        assertFalse(result.isValid());
                        assertTrue(result.hasFieldError("name"));
                    })
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should handle missing required field")
        void shouldHandleMissingRequiredField() {
            TestProcessPlugin plugin = new TestProcessPlugin();
            
            ProcessExecutionContext context = createContext(Map.of("amount", 100.0));
            
            StepVerifier.create(plugin.validate(context))
                    .assertNext(result -> {
                        assertFalse(result.isValid());
                        assertTrue(result.hasFieldError("name"));
                    })
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Compensation Tests")
    class CompensationTests {
        
        @Test
        @DisplayName("Should execute compensation logic")
        void shouldExecuteCompensation() {
            CompensatingProcessPlugin plugin = new CompensatingProcessPlugin();
            
            ProcessExecutionContext context = createContext(Map.of("name", "test"));
            
            StepVerifier.create(plugin.compensate(context))
                    .assertNext(result -> {
                        assertTrue(result.isSuccess());
                        @SuppressWarnings("unchecked")
                        Map<String, Object> output = (Map<String, Object>) result.getOutput();
                        assertEquals(true, output.get("compensated"));
                        @SuppressWarnings("unchecked")
                        Map<String, Object> compensationResult = (Map<String, Object>) output.get("result");
                        assertEquals(true, compensationResult.get("reversed"));
                        assertEquals("txn-123", compensationResult.get("originalTxnId"));
                    })
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Default compensation should return success with compensated flag")
        void defaultCompensationShouldReturnSuccess() {
            TestProcessPlugin plugin = new TestProcessPlugin();
            
            ProcessExecutionContext context = createContext(Map.of("name", "test"));
            
            StepVerifier.create(plugin.compensate(context))
                    .assertNext(result -> {
                        assertTrue(result.isSuccess());
                        @SuppressWarnings("unchecked")
                        Map<String, Object> output = (Map<String, Object>) result.getOutput();
                        assertEquals(true, output.get("compensated"));
                    })
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle runtime exceptions gracefully")
        void shouldHandleRuntimeExceptions() {
            @FireflyProcess(id = "throwing-process", version = "1.0.0")
            class ThrowingPlugin extends AbstractProcessPlugin<TestRequest, TestResponse> {
                @Override
                protected Mono<TestResponse> doExecute(ProcessExecutionContext context, TestRequest input) {
                    return Mono.error(new RuntimeException("Unexpected error"));
                }
            }
            
            ThrowingPlugin plugin = new ThrowingPlugin();
            ProcessExecutionContext context = createContext(Map.of("name", "test"));
            
            StepVerifier.create(plugin.execute(context))
                    .assertNext(result -> {
                        assertFalse(result.isSuccess());
                        assertEquals(ProcessResult.Status.TECHNICAL_ERROR, result.getStatus());
                        // Error code is now the execution phase for better debugging
                        assertEquals("EXECUTION", result.getErrorCode());
                        assertTrue(result.getErrorMessage().contains("Unexpected error"));
                    })
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should handle input conversion errors")
        void shouldHandleInputConversionErrors() {
            TestProcessPlugin plugin = new TestProcessPlugin();
            
            // Invalid input that can't be converted to TestRequest
            ProcessExecutionContext context = createContext(Map.of(
                    "invalid_field", "value"
            ));
            
            // This should still execute but with null fields
            StepVerifier.create(plugin.validate(context))
                    .assertNext(result -> {
                        // Name will be null which should fail validation
                        assertFalse(result.isValid());
                    })
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Business Error Helper Tests")
    class BusinessErrorHelperTests {
        
        @Test
        @DisplayName("businessError() should return proper error result")
        void businessErrorShouldReturnProperResult() {
            ErrorProcessPlugin plugin = new ErrorProcessPlugin();
            
            ProcessExecutionContext context = createContext(Map.of("name", "fail"));
            
            StepVerifier.create(plugin.execute(context))
                    .assertNext(result -> {
                        assertFalse(result.isSuccess());
                        assertEquals(ProcessResult.Status.BUSINESS_ERROR, result.getStatus());
                        assertEquals("BUSINESS_RULE_VIOLATED", result.getErrorCode());
                    })
                    .verifyComplete();
        }
    }
}
