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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessMetadata Tests")
class ProcessMetadataTest {
    
    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {
        
        @Test
        @DisplayName("minimal() should create metadata with only required fields")
        void minimalShouldCreateMetadataWithOnlyRequiredFields() {
            ProcessMetadata metadata = ProcessMetadata.minimal("account-creation", "1.0.0");
            
            assertEquals("account-creation", metadata.getProcessId());
            assertEquals("1.0.0", metadata.getVersion());
            assertNull(metadata.getName());
            assertNull(metadata.getDescription());
            assertFalse(metadata.isVanilla());
            assertFalse(metadata.isDeprecated());
        }
    }
    
    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {
        
        @Test
        @DisplayName("builder should create metadata with all fields")
        void builderShouldCreateMetadataWithAllFields() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("vanilla-account-creation")
                    .name("Standard Account Creation")
                    .version("1.2.3")
                    .description("Creates a new account with standard KYC verification")
                    .category("ACCOUNTS")
                    .capability("ACCOUNT_CREATION")
                    .capability("KYC_VERIFICATION")
                    .requiredPermission("accounts:create")
                    .requiredPermission("kyc:verify")
                    .requiredRole("BANK_TELLER")
                    .requiredFeature("ACCOUNTS_V2")
                    .tag("banking")
                    .tag("core")
                    .property("timeout", 30000)
                    .property("retryable", true)
                    .sourceType("spring-bean")
                    .sourceUri("org.fireflyframework.AccountCreationPlugin")
                    .vanilla(true)
                    .deprecated(false)
                    .inputType(Map.class)
                    .outputType(String.class)
                    .build();
            
            assertEquals("vanilla-account-creation", metadata.getProcessId());
            assertEquals("Standard Account Creation", metadata.getName());
            assertEquals("1.2.3", metadata.getVersion());
            assertEquals("Creates a new account with standard KYC verification", metadata.getDescription());
            assertEquals("ACCOUNTS", metadata.getCategory());
            assertTrue(metadata.getCapabilities().contains("ACCOUNT_CREATION"));
            assertTrue(metadata.getCapabilities().contains("KYC_VERIFICATION"));
            assertTrue(metadata.getRequiredPermissions().contains("accounts:create"));
            assertTrue(metadata.getRequiredPermissions().contains("kyc:verify"));
            assertTrue(metadata.getRequiredRoles().contains("BANK_TELLER"));
            assertTrue(metadata.getRequiredFeatures().contains("ACCOUNTS_V2"));
            assertTrue(metadata.getTags().contains("banking"));
            assertTrue(metadata.getTags().contains("core"));
            assertEquals(30000, metadata.getProperties().get("timeout"));
            assertEquals(true, metadata.getProperties().get("retryable"));
            assertEquals("spring-bean", metadata.getSourceType());
            assertEquals("org.fireflyframework.AccountCreationPlugin", metadata.getSourceUri());
            assertTrue(metadata.isVanilla());
            assertFalse(metadata.isDeprecated());
            assertEquals(Map.class, metadata.getInputType());
            assertEquals(String.class, metadata.getOutputType());
        }
        
        @Test
        @DisplayName("builder should use default values for vanilla and deprecated")
        void builderShouldUseDefaultValues() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .build();
            
            assertFalse(metadata.isVanilla());
            assertFalse(metadata.isDeprecated());
        }
        
        @Test
        @DisplayName("toBuilder should allow modifying existing metadata")
        void toBuilderShouldAllowModifyingMetadata() {
            ProcessMetadata original = ProcessMetadata.builder()
                    .processId("test-process")
                    .version("1.0.0")
                    .name("Test")
                    .build();
            
            ProcessMetadata modified = original.toBuilder()
                    .version("2.0.0")
                    .deprecated(true)
                    .replacedBy("new-test-process")
                    .build();
            
            assertEquals("test-process", modified.getProcessId());
            assertEquals("2.0.0", modified.getVersion());
            assertEquals("Test", modified.getName());
            assertTrue(modified.isDeprecated());
            assertEquals("new-test-process", modified.getReplacedBy());
        }
    }
    
    @Nested
    @DisplayName("Capability Tests")
    class CapabilityTests {
        
        @Test
        @DisplayName("hasCapability() should return true when capability exists")
        void hasCapabilityShouldReturnTrueWhenCapabilityExists() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .capability("ACCOUNT_CREATION")
                    .capability("KYC_VERIFICATION")
                    .build();
            
            assertTrue(metadata.hasCapability("ACCOUNT_CREATION"));
            assertTrue(metadata.hasCapability("KYC_VERIFICATION"));
        }
        
        @Test
        @DisplayName("hasCapability() should return false when capability doesn't exist")
        void hasCapabilityShouldReturnFalseWhenCapabilityDoesNotExist() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .capability("ACCOUNT_CREATION")
                    .build();
            
            assertFalse(metadata.hasCapability("PAYMENTS"));
        }
        
        @Test
        @DisplayName("hasCapability() should return false when capabilities is null")
        void hasCapabilityShouldReturnFalseWhenCapabilitiesIsNull() {
            ProcessMetadata metadata = ProcessMetadata.minimal("test", "1.0.0");
            
            assertFalse(metadata.hasCapability("ANY_CAPABILITY"));
        }
    }
    
    @Nested
    @DisplayName("Permission Tests")
    class PermissionTests {
        
        @Test
        @DisplayName("requiresPermission() should return true when permission is required")
        void requiresPermissionShouldReturnTrueWhenRequired() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .requiredPermission("accounts:create")
                    .requiredPermission("accounts:read")
                    .build();
            
            assertTrue(metadata.requiresPermission("accounts:create"));
            assertTrue(metadata.requiresPermission("accounts:read"));
        }
        
        @Test
        @DisplayName("requiresPermission() should return false when permission not required")
        void requiresPermissionShouldReturnFalseWhenNotRequired() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .requiredPermission("accounts:read")
                    .build();
            
            assertFalse(metadata.requiresPermission("accounts:delete"));
        }
        
        @Test
        @DisplayName("requiresPermission() should return false when permissions is null")
        void requiresPermissionShouldReturnFalseWhenPermissionsIsNull() {
            ProcessMetadata metadata = ProcessMetadata.minimal("test", "1.0.0");
            
            assertFalse(metadata.requiresPermission("any:permission"));
        }
        
        @Test
        @DisplayName("hasRequiredPermissions() should return true when permissions exist")
        void hasRequiredPermissionsShouldReturnTrueWhenPermissionsExist() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .requiredPermission("accounts:create")
                    .build();
            
            assertTrue(metadata.hasRequiredPermissions());
        }
        
        @Test
        @DisplayName("hasRequiredPermissions() should return false when no permissions")
        void hasRequiredPermissionsShouldReturnFalseWhenNoPermissions() {
            ProcessMetadata metadata = ProcessMetadata.minimal("test", "1.0.0");
            
            assertFalse(metadata.hasRequiredPermissions());
        }
    }
    
    @Nested
    @DisplayName("Role Tests")
    class RoleTests {
        
        @Test
        @DisplayName("hasRequiredRoles() should return true when roles exist")
        void hasRequiredRolesShouldReturnTrueWhenRolesExist() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .requiredRole("ADMIN")
                    .requiredRole("MANAGER")
                    .build();
            
            assertTrue(metadata.hasRequiredRoles());
            assertEquals(2, metadata.getRequiredRoles().size());
        }
        
        @Test
        @DisplayName("hasRequiredRoles() should return false when no roles")
        void hasRequiredRolesShouldReturnFalseWhenNoRoles() {
            ProcessMetadata metadata = ProcessMetadata.minimal("test", "1.0.0");
            
            assertFalse(metadata.hasRequiredRoles());
        }
    }
    
    @Nested
    @DisplayName("Deprecation Tests")
    class DeprecationTests {
        
        @Test
        @DisplayName("deprecated metadata should include replacement info")
        void deprecatedMetadataShouldIncludeReplacementInfo() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("old-account-creation")
                    .version("1.0.0")
                    .deprecated(true)
                    .replacedBy("new-account-creation")
                    .build();
            
            assertTrue(metadata.isDeprecated());
            assertEquals("new-account-creation", metadata.getReplacedBy());
        }
    }
    
    @Nested
    @DisplayName("Source Information Tests")
    class SourceInformationTests {
        
        @Test
        @DisplayName("should store spring-bean source information")
        void shouldStoreSpringBeanSourceInfo() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .sourceType("spring-bean")
                    .sourceUri("org.fireflyframework.plugins.TestPlugin")
                    .build();
            
            assertEquals("spring-bean", metadata.getSourceType());
            assertEquals("org.fireflyframework.plugins.TestPlugin", metadata.getSourceUri());
        }
        
        @Test
        @DisplayName("should store jar source information")
        void shouldStoreJarSourceInfo() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .sourceType("jar")
                    .sourceUri("/plugins/custom-plugin-1.0.0.jar")
                    .build();
            
            assertEquals("jar", metadata.getSourceType());
            assertEquals("/plugins/custom-plugin-1.0.0.jar", metadata.getSourceUri());
        }
        
        @Test
        @DisplayName("should store remote source information")
        void shouldStoreRemoteSourceInfo() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .sourceType("remote-maven")
                    .sourceUri("https://maven.example.com/repo")
                    .build();
            
            assertEquals("remote-maven", metadata.getSourceType());
            assertEquals("https://maven.example.com/repo", metadata.getSourceUri());
        }
    }
    
    @Nested
    @DisplayName("Properties Tests")
    class PropertiesTests {
        
        @Test
        @DisplayName("should store custom properties")
        void shouldStoreCustomProperties() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .property("timeout", 30000)
                    .property("retryable", true)
                    .property("maxRetries", 3)
                    .build();
            
            Map<String, Object> props = metadata.getProperties();
            
            assertEquals(3, props.size());
            assertEquals(30000, props.get("timeout"));
            assertEquals(true, props.get("retryable"));
            assertEquals(3, props.get("maxRetries"));
        }
    }
    
    @Nested
    @DisplayName("Input/Output Type Tests")
    class TypeTests {
        
        @Test
        @DisplayName("should store input and output types")
        void shouldStoreInputAndOutputTypes() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .processId("test")
                    .version("1.0.0")
                    .inputType(Map.class)
                    .outputType(Set.class)
                    .build();
            
            assertEquals(Map.class, metadata.getInputType());
            assertEquals(Set.class, metadata.getOutputType());
        }
    }
}
