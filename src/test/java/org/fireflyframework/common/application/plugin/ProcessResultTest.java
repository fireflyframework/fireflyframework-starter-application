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

import lombok.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessResult Tests")
class ProcessResultTest {
    
    @Nested
    @DisplayName("Success Factory Methods")
    class SuccessFactoryMethodTests {
        
        @Test
        @DisplayName("success() should create successful result without output")
        void successShouldCreateSuccessfulResultWithoutOutput() {
            ProcessResult result = ProcessResult.success();
            
            assertTrue(result.isSuccess());
            assertFalse(result.isFailed());
            assertFalse(result.isPending());
            assertEquals(ProcessResult.Status.SUCCESS, result.getStatus());
            assertNull(result.getOutput());
        }
        
        @Test
        @DisplayName("success(output) should create successful result with output")
        void successWithOutputShouldCreateSuccessfulResultWithOutput() {
            String output = "Account created";
            
            ProcessResult result = ProcessResult.success(output);
            
            assertTrue(result.isSuccess());
            assertEquals("Account created", result.getOutput());
        }
        
        @Test
        @DisplayName("success() should allow complex output objects")
        void successShouldAllowComplexOutputObjects() {
            Map<String, Object> account = new HashMap<>();
            account.put("id", "ACC-001");
            account.put("balance", BigDecimal.valueOf(1000.00));
            
            ProcessResult result = ProcessResult.success(account);
            
            assertTrue(result.isSuccess());
            assertEquals(account, result.getOutput());
        }
    }
    
    @Nested
    @DisplayName("Failure Factory Methods")
    class FailureFactoryMethodTests {
        
        @Test
        @DisplayName("failed(errorCode, message) should create technical error")
        void failedWithCodeAndMessageShouldCreateTechnicalError() {
            ProcessResult result = ProcessResult.failed("DB_ERROR", "Database connection failed");
            
            assertTrue(result.isFailed());
            assertFalse(result.isSuccess());
            assertEquals(ProcessResult.Status.TECHNICAL_ERROR, result.getStatus());
            assertEquals("DB_ERROR", result.getErrorCode());
            assertEquals("Database connection failed", result.getErrorMessage());
        }
        
        @Test
        @DisplayName("failed(errorCode) should create technical error without message")
        void failedWithCodeShouldCreateTechnicalErrorWithoutMessage() {
            ProcessResult result = ProcessResult.failed("TIMEOUT");
            
            assertTrue(result.isFailed());
            assertEquals("TIMEOUT", result.getErrorCode());
            assertNull(result.getErrorMessage());
        }
        
        @Test
        @DisplayName("failed(exception) should create technical error from exception")
        void failedWithExceptionShouldCreateTechnicalError() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid input");
            
            ProcessResult result = ProcessResult.failed(exception);
            
            assertTrue(result.isFailed());
            assertEquals(ProcessResult.Status.TECHNICAL_ERROR, result.getStatus());
            assertEquals("IllegalArgumentException", result.getErrorCode());
            assertEquals("Invalid input", result.getErrorMessage());
            assertSame(exception, result.getException());
        }
        
        @Test
        @DisplayName("technicalError() should create technical error with exception and code")
        void technicalErrorShouldCreateWithExceptionAndCode() {
            RuntimeException exception = new RuntimeException("Connection lost");
            
            ProcessResult result = ProcessResult.technicalError(exception, "CONN_ERROR");
            
            assertTrue(result.isFailed());
            assertEquals(ProcessResult.Status.TECHNICAL_ERROR, result.getStatus());
            assertEquals("CONN_ERROR", result.getErrorCode());
            assertEquals("Connection lost", result.getErrorMessage());
            assertSame(exception, result.getException());
        }
        
        @Test
        @DisplayName("businessError() should create business error result")
        void businessErrorShouldCreateBusinessErrorResult() {
            ProcessResult result = ProcessResult.businessError("INSUFFICIENT_BALANCE", "Not enough funds");
            
            assertTrue(result.isFailed());
            assertEquals(ProcessResult.Status.BUSINESS_ERROR, result.getStatus());
            assertEquals("INSUFFICIENT_BALANCE", result.getErrorCode());
            assertEquals("Not enough funds", result.getErrorMessage());
        }
    }
    
    @Nested
    @DisplayName("Other Status Factory Methods")
    class OtherStatusFactoryMethodTests {
        
        @Test
        @DisplayName("pending() should create pending result")
        void pendingShouldCreatePendingResult() {
            Map<String, String> pendingInfo = Map.of("approvalId", "APR-001");
            
            ProcessResult result = ProcessResult.pending(pendingInfo);
            
            assertTrue(result.isPending());
            assertFalse(result.isSuccess());
            assertFalse(result.isFailed());
            assertEquals(ProcessResult.Status.PENDING, result.getStatus());
            assertEquals(pendingInfo, result.getOutput());
        }
        
        @Test
        @DisplayName("partial() should create partial result")
        void partialShouldCreatePartialResult() {
            String partialOutput = "Partial data";
            
            ProcessResult result = ProcessResult.partial(partialOutput, "Some items failed to process");
            
            assertEquals(ProcessResult.Status.PARTIAL, result.getStatus());
            assertEquals("Partial data", result.getOutput());
            assertEquals("Some items failed to process", result.getErrorMessage());
        }
    }
    
    @Nested
    @DisplayName("Typed Output Access")
    class TypedOutputAccessTests {
        
        @Test
        @DisplayName("getOutput(Class) should return typed output when type matches")
        void getOutputShouldReturnTypedOutputWhenTypeMatches() {
            ProcessResult result = ProcessResult.success("test string");
            
            String output = result.getOutput(String.class);
            
            assertEquals("test string", output);
        }
        
        @Test
        @DisplayName("getOutput(Class) should return null when output is null")
        void getOutputShouldReturnNullWhenOutputIsNull() {
            ProcessResult result = ProcessResult.success();
            
            String output = result.getOutput(String.class);
            
            assertNull(output);
        }
        
        @Test
        @DisplayName("getOutput(Class) should convert map to typed object")
        void getOutputShouldConvertMapToTypedObject() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", "Test Account");
            map.put("amount", 1000);
            
            ProcessResult result = ProcessResult.success(map);
            
            TestOutputDto output = result.getOutput(TestOutputDto.class);
            
            assertEquals("Test Account", output.getName());
            assertEquals(1000, output.getAmount());
        }
        
        @Test
        @DisplayName("getOutput(Class) should throw when conversion fails")
        void getOutputShouldThrowWhenConversionFails() {
            ProcessResult result = ProcessResult.success("not a number");
            
            assertThrows(IllegalArgumentException.class, () -> result.getOutput(Integer.class));
        }
    }
    
    @Nested
    @DisplayName("Metadata Tests")
    class MetadataTests {
        
        @Test
        @DisplayName("builder should allow adding metadata")
        void builderShouldAllowAddingMetadata() {
            ProcessResult result = ProcessResult.builder()
                    .status(ProcessResult.Status.SUCCESS)
                    .metadata("auditId", "AUD-001")
                    .metadata("userId", "USR-001")
                    .build();
            
            assertEquals(2, result.getMetadata().size());
            assertEquals("AUD-001", result.getMetadata().get("auditId"));
            assertEquals("USR-001", result.getMetadata().get("userId"));
        }
        
        @Test
        @DisplayName("getMetadata(key, type) should return typed metadata value")
        void getMetadataShouldReturnTypedMetadataValue() {
            ProcessResult result = ProcessResult.builder()
                    .status(ProcessResult.Status.SUCCESS)
                    .metadata("count", 42)
                    .build();
            
            Integer count = result.getMetadata("count", Integer.class);
            
            assertEquals(42, count);
        }
        
        @Test
        @DisplayName("getMetadata(key, type) should return null for missing key")
        void getMetadataShouldReturnNullForMissingKey() {
            ProcessResult result = ProcessResult.success();
            
            String value = result.getMetadata("missing", String.class);
            
            assertNull(value);
        }
        
        @Test
        @DisplayName("getMetadata(key, type) should return null for type mismatch")
        void getMetadataShouldReturnNullForTypeMismatch() {
            ProcessResult result = ProcessResult.builder()
                    .status(ProcessResult.Status.SUCCESS)
                    .metadata("count", "not a number")
                    .build();
            
            Integer count = result.getMetadata("count", Integer.class);
            
            assertNull(count);
        }
    }
    
    @Nested
    @DisplayName("Timing Tests")
    class TimingTests {
        
        @Test
        @DisplayName("withTiming() should add execution timing")
        void withTimingShouldAddExecutionTiming() throws InterruptedException {
            long startTime = System.currentTimeMillis();
            Thread.sleep(10); // Small delay to ensure measurable time
            ProcessResult original = ProcessResult.success("output");
            
            ProcessResult withTiming = original.withTiming(startTime, "EXEC-001");
            
            assertEquals("EXEC-001", withTiming.getExecutionId());
            assertTrue(withTiming.getExecutionTimeMs() >= 10);
            assertEquals("output", withTiming.getOutput()); // Original data preserved
        }
        
        @Test
        @DisplayName("withTiming() should not modify original result")
        void withTimingShouldNotModifyOriginalResult() {
            ProcessResult original = ProcessResult.success();
            long startTime = System.currentTimeMillis();
            
            ProcessResult withTiming = original.withTiming(startTime, "EXEC-001");
            
            assertNull(original.getExecutionId());
            assertEquals(0, original.getExecutionTimeMs());
            assertNotSame(original, withTiming);
        }
    }
    
    @Nested
    @DisplayName("Status Check Tests")
    class StatusCheckTests {
        
        @Test
        @DisplayName("isSuccess() should return true only for SUCCESS status")
        void isSuccessShouldReturnTrueOnlyForSuccessStatus() {
            assertTrue(ProcessResult.success().isSuccess());
            assertFalse(ProcessResult.failed("ERR").isSuccess());
            assertFalse(ProcessResult.businessError("BUS", "msg").isSuccess());
            assertFalse(ProcessResult.pending(null).isSuccess());
            assertFalse(ProcessResult.partial(null, "msg").isSuccess());
        }
        
        @Test
        @DisplayName("isFailed() should return true for BUSINESS_ERROR and TECHNICAL_ERROR")
        void isFailedShouldReturnTrueForErrorStatuses() {
            assertFalse(ProcessResult.success().isFailed());
            assertTrue(ProcessResult.failed("ERR").isFailed());
            assertTrue(ProcessResult.businessError("BUS", "msg").isFailed());
            assertFalse(ProcessResult.pending(null).isFailed());
            assertFalse(ProcessResult.partial(null, "msg").isFailed());
        }
        
        @Test
        @DisplayName("isPending() should return true only for PENDING status")
        void isPendingShouldReturnTrueOnlyForPendingStatus() {
            assertFalse(ProcessResult.success().isPending());
            assertFalse(ProcessResult.failed("ERR").isPending());
            assertTrue(ProcessResult.pending(null).isPending());
            assertFalse(ProcessResult.partial(null, "msg").isPending());
        }
    }
    
    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {
        
        @Test
        @DisplayName("builder should create result with all fields")
        void builderShouldCreateResultWithAllFields() {
            RuntimeException exception = new RuntimeException("Test");
            
            ProcessResult result = ProcessResult.builder()
                    .status(ProcessResult.Status.TECHNICAL_ERROR)
                    .output("partial data")
                    .errorCode("ERR-001")
                    .errorMessage("Something went wrong")
                    .exception(exception)
                    .metadata("key1", "value1")
                    .executionTimeMs(150)
                    .executionId("EXEC-001")
                    .build();
            
            assertEquals(ProcessResult.Status.TECHNICAL_ERROR, result.getStatus());
            assertEquals("partial data", result.getOutput());
            assertEquals("ERR-001", result.getErrorCode());
            assertEquals("Something went wrong", result.getErrorMessage());
            assertSame(exception, result.getException());
            assertEquals("value1", result.getMetadata().get("key1"));
            assertEquals(150, result.getExecutionTimeMs());
            assertEquals("EXEC-001", result.getExecutionId());
        }
        
        @Test
        @DisplayName("toBuilder should allow modifying existing result")
        void toBuilderShouldAllowModifyingExistingResult() {
            ProcessResult original = ProcessResult.success("output");
            
            ProcessResult modified = original.toBuilder()
                    .executionId("EXEC-002")
                    .build();
            
            assertEquals("output", modified.getOutput());
            assertEquals("EXEC-002", modified.getExecutionId());
            assertNull(original.getExecutionId()); // Original unchanged
        }
    }
    
    // Test DTO for conversion tests
    @Value
    static class TestOutputDto {
        String name;
        int amount;
    }
}
