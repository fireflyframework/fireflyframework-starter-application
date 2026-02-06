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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProcessPluginRegistry Tests")
class ProcessPluginRegistryTest {
    
    private ProcessPluginRegistry registry;
    
    @BeforeEach
    void setUp() {
        registry = new ProcessPluginRegistry();
    }
    
    // Test helper to create mock plugins
    private ProcessPlugin createMockPlugin(String processId, String version) {
        return createMockPlugin(processId, version, null);
    }
    
    private ProcessPlugin createMockPlugin(String processId, String version, ProcessMetadata metadata) {
        return new ProcessPlugin() {
            @Override
            public String getProcessId() {
                return processId;
            }
            
            @Override
            public String getVersion() {
                return version;
            }
            
            @Override
            public ProcessMetadata getMetadata() {
                return metadata;
            }
            
            @Override
            public Mono<ProcessResult> execute(ProcessExecutionContext context) {
                return Mono.just(ProcessResult.success(Map.of()));
            }
        };
    }
    
    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {
        
        @Test
        @DisplayName("Should register a plugin successfully")
        void shouldRegisterPlugin() {
            ProcessPlugin plugin = createMockPlugin("test-process", "1.0.0");
            
            StepVerifier.create(registry.register(plugin))
                    .verifyComplete();
            
            assertTrue(registry.contains("test-process"));
            assertTrue(registry.contains("test-process", "1.0.0"));
            assertEquals(1, registry.size());
        }
        
        @Test
        @DisplayName("Should register multiple versions of the same plugin")
        void shouldRegisterMultipleVersions() {
            ProcessPlugin v1 = createMockPlugin("test-process", "1.0.0");
            ProcessPlugin v2 = createMockPlugin("test-process", "2.0.0");
            ProcessPlugin v3 = createMockPlugin("test-process", "1.5.0");
            
            StepVerifier.create(registry.register(v1)).verifyComplete();
            StepVerifier.create(registry.register(v2)).verifyComplete();
            StepVerifier.create(registry.register(v3)).verifyComplete();
            
            assertEquals(1, registry.size());  // Only one processId
            assertEquals(3, registry.totalVersionCount());
            assertTrue(registry.contains("test-process", "1.0.0"));
            assertTrue(registry.contains("test-process", "1.5.0"));
            assertTrue(registry.contains("test-process", "2.0.0"));
        }
        
        @Test
        @DisplayName("Should replace existing plugin with same version")
        void shouldReplaceExistingVersion() {
            ProcessPlugin v1a = createMockPlugin("test-process", "1.0.0");
            ProcessPlugin v1b = createMockPlugin("test-process", "1.0.0");
            
            StepVerifier.create(registry.register(v1a)).verifyComplete();
            StepVerifier.create(registry.register(v1b)).verifyComplete();
            
            assertEquals(1, registry.size());
            assertEquals(1, registry.totalVersionCount());
        }
        
        @Test
        @DisplayName("Should register plugins with capabilities")
        void shouldRegisterPluginWithCapabilities() {
            ProcessMetadata metadata = ProcessMetadata.builder()
                    .capabilities(Set.of("payment", "transfer"))
                    .build();
            ProcessPlugin plugin = createMockPlugin("payment-process", "1.0.0", metadata);
            
            StepVerifier.create(registry.register(plugin)).verifyComplete();
            
            StepVerifier.create(registry.findByCapability("payment"))
                    .expectNextMatches(p -> p.getProcessId().equals("payment-process"))
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Version Resolution Tests")
    class VersionResolutionTests {
        
        @Test
        @DisplayName("Should return latest version when no version specified")
        void shouldReturnLatestVersion() {
            ProcessPlugin v1 = createMockPlugin("test-process", "1.0.0");
            ProcessPlugin v2 = createMockPlugin("test-process", "2.0.0");
            ProcessPlugin v15 = createMockPlugin("test-process", "1.5.0");
            
            registry.register(v1).block();
            registry.register(v2).block();
            registry.register(v15).block();
            
            Optional<ProcessPlugin> result = registry.get("test-process");
            
            assertTrue(result.isPresent());
            assertEquals("2.0.0", result.get().getVersion());
        }
        
        @Test
        @DisplayName("Should return specific version when requested")
        void shouldReturnSpecificVersion() {
            ProcessPlugin v1 = createMockPlugin("test-process", "1.0.0");
            ProcessPlugin v2 = createMockPlugin("test-process", "2.0.0");
            
            registry.register(v1).block();
            registry.register(v2).block();
            
            Optional<ProcessPlugin> result = registry.get("test-process", "1.0.0");
            
            assertTrue(result.isPresent());
            assertEquals("1.0.0", result.get().getVersion());
        }
        
        @Test
        @DisplayName("Should handle semantic version ordering correctly")
        void shouldHandleSemanticVersionOrdering() {
            // Register out of order
            registry.register(createMockPlugin("test", "1.10.0")).block();
            registry.register(createMockPlugin("test", "1.2.0")).block();
            registry.register(createMockPlugin("test", "1.9.0")).block();
            registry.register(createMockPlugin("test", "2.0.0")).block();
            registry.register(createMockPlugin("test", "1.0.0")).block();
            
            Optional<ProcessPlugin> latest = registry.get("test");
            assertTrue(latest.isPresent());
            assertEquals("2.0.0", latest.get().getVersion());
        }
        
        @Test
        @DisplayName("Should handle SNAPSHOT versions as lower priority")
        void shouldHandleSnapshotVersions() {
            registry.register(createMockPlugin("test", "2.0.0-SNAPSHOT")).block();
            registry.register(createMockPlugin("test", "1.9.0")).block();
            registry.register(createMockPlugin("test", "2.0.0")).block();
            
            Optional<ProcessPlugin> latest = registry.get("test");
            assertTrue(latest.isPresent());
            assertEquals("2.0.0", latest.get().getVersion());
        }
        
        @Test
        @DisplayName("Should handle pre-release version ordering")
        void shouldHandlePreReleaseVersionOrdering() {
            registry.register(createMockPlugin("test", "1.0.0-ALPHA")).block();
            registry.register(createMockPlugin("test", "1.0.0-BETA")).block();
            registry.register(createMockPlugin("test", "1.0.0-RC1")).block();
            registry.register(createMockPlugin("test", "1.0.0")).block();
            registry.register(createMockPlugin("test", "1.0.0-SNAPSHOT")).block();
            
            Optional<ProcessPlugin> latest = registry.get("test");
            assertTrue(latest.isPresent());
            assertEquals("1.0.0", latest.get().getVersion());
        }
        
        @Test
        @DisplayName("Should return empty for non-existent process")
        void shouldReturnEmptyForNonExistent() {
            Optional<ProcessPlugin> result = registry.get("non-existent");
            assertTrue(result.isEmpty());
        }
        
        @Test
        @DisplayName("Should return empty for non-existent version")
        void shouldReturnEmptyForNonExistentVersion() {
            registry.register(createMockPlugin("test", "1.0.0")).block();
            
            Optional<ProcessPlugin> result = registry.get("test", "2.0.0");
            assertTrue(result.isEmpty());
        }
    }
    
    @Nested
    @DisplayName("Unregistration Tests")
    class UnregistrationTests {
        
        @Test
        @DisplayName("Should unregister all versions of a plugin")
        void shouldUnregisterAllVersions() {
            registry.register(createMockPlugin("test", "1.0.0")).block();
            registry.register(createMockPlugin("test", "2.0.0")).block();
            
            StepVerifier.create(registry.unregister("test"))
                    .expectNextMatches(plugins -> plugins.size() == 2)
                    .verifyComplete();
            
            assertFalse(registry.contains("test"));
            assertEquals(0, registry.size());
        }
        
        @Test
        @DisplayName("Should unregister specific version")
        void shouldUnregisterSpecificVersion() {
            registry.register(createMockPlugin("test", "1.0.0")).block();
            registry.register(createMockPlugin("test", "2.0.0")).block();
            
            StepVerifier.create(registry.unregister("test", "1.0.0"))
                    .expectNextMatches(plugin -> plugin.getVersion().equals("1.0.0"))
                    .verifyComplete();
            
            assertTrue(registry.contains("test"));
            assertFalse(registry.contains("test", "1.0.0"));
            assertTrue(registry.contains("test", "2.0.0"));
        }
        
        @Test
        @DisplayName("Should clean up processId when last version unregistered")
        void shouldCleanupOnLastVersionUnregister() {
            registry.register(createMockPlugin("test", "1.0.0")).block();
            
            registry.unregister("test", "1.0.0").block();
            
            assertFalse(registry.contains("test"));
            assertEquals(0, registry.size());
        }
        
        @Test
        @DisplayName("Should return null when unregistering non-existent plugin")
        void shouldReturnNullForNonExistent() {
            StepVerifier.create(registry.unregister("non-existent"))
                    .expectNext()  // null wrapped in Mono
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Query Tests")
    class QueryTests {
        
        @Test
        @DisplayName("Should get all registered plugins (latest versions)")
        void shouldGetAllPlugins() {
            registry.register(createMockPlugin("process-a", "1.0.0")).block();
            registry.register(createMockPlugin("process-a", "2.0.0")).block();
            registry.register(createMockPlugin("process-b", "1.0.0")).block();
            
            Collection<ProcessPlugin> all = registry.getAll();
            
            assertEquals(2, all.size());
            assertTrue(all.stream().anyMatch(p -> p.getProcessId().equals("process-a") && p.getVersion().equals("2.0.0")));
            assertTrue(all.stream().anyMatch(p -> p.getProcessId().equals("process-b")));
        }
        
        @Test
        @DisplayName("Should get all versions of a specific process")
        void shouldGetAllVersions() {
            registry.register(createMockPlugin("test", "1.0.0")).block();
            registry.register(createMockPlugin("test", "2.0.0")).block();
            registry.register(createMockPlugin("test", "1.5.0")).block();
            
            Collection<ProcessPlugin> versions = registry.getAllVersions("test");
            
            assertEquals(3, versions.size());
        }
        
        @Test
        @DisplayName("Should find plugins by capability")
        void shouldFindByCapability() {
            ProcessMetadata paymentMeta = ProcessMetadata.builder()
                    .capabilities(Set.of("payment", "notification"))
                    .build();
            ProcessMetadata transferMeta = ProcessMetadata.builder()
                    .capabilities(Set.of("transfer", "notification"))
                    .build();
            
            registry.register(createMockPlugin("payment-process", "1.0.0", paymentMeta)).block();
            registry.register(createMockPlugin("transfer-process", "1.0.0", transferMeta)).block();
            
            StepVerifier.create(registry.findByCapability("notification").collectList())
                    .expectNextMatches(list -> list.size() == 2)
                    .verifyComplete();
            
            StepVerifier.create(registry.findByCapability("payment").collectList())
                    .expectNextMatches(list -> list.size() == 1)
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should find plugins by category")
        void shouldFindByCategory() {
            ProcessMetadata accountMeta = ProcessMetadata.builder()
                    .category("ACCOUNTS")
                    .build();
            ProcessMetadata paymentMeta = ProcessMetadata.builder()
                    .category("PAYMENTS")
                    .build();
            
            registry.register(createMockPlugin("create-account", "1.0.0", accountMeta)).block();
            registry.register(createMockPlugin("process-payment", "1.0.0", paymentMeta)).block();
            
            StepVerifier.create(registry.findByCategory("ACCOUNTS").collectList())
                    .expectNextMatches(list -> list.size() == 1 && 
                            list.get(0).getProcessId().equals("create-account"))
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should find vanilla plugins")
        void shouldFindVanillaPlugins() {
            ProcessMetadata vanillaMeta = ProcessMetadata.builder()
                    .vanilla(true)
                    .build();
            ProcessMetadata customMeta = ProcessMetadata.builder()
                    .vanilla(false)
                    .build();
            
            registry.register(createMockPlugin("vanilla-process", "1.0.0", vanillaMeta)).block();
            registry.register(createMockPlugin("custom-process", "1.0.0", customMeta)).block();
            
            StepVerifier.create(registry.findVanillaPlugins().collectList())
                    .expectNextMatches(list -> list.size() == 1 && 
                            list.get(0).getProcessId().equals("vanilla-process"))
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Reactive API Tests")
    class ReactiveApiTests {
        
        @Test
        @DisplayName("Should get plugin reactively")
        void shouldGetReactively() {
            registry.register(createMockPlugin("test", "1.0.0")).block();
            
            StepVerifier.create(registry.getReactive("test"))
                    .expectNextMatches(p -> p.getProcessId().equals("test"))
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should return empty Mono for non-existent plugin")
        void shouldReturnEmptyMonoForNonExistent() {
            StepVerifier.create(registry.getReactive("non-existent"))
                    .verifyComplete();
        }
        
        @Test
        @DisplayName("Should get specific version reactively")
        void shouldGetVersionReactively() {
            registry.register(createMockPlugin("test", "1.0.0")).block();
            registry.register(createMockPlugin("test", "2.0.0")).block();
            
            StepVerifier.create(registry.getReactive("test", "1.0.0"))
                    .expectNextMatches(p -> p.getVersion().equals("1.0.0"))
                    .verifyComplete();
        }
    }
    
    @Nested
    @DisplayName("Clear Tests")
    class ClearTests {
        
        @Test
        @DisplayName("Should clear all plugins")
        void shouldClearAllPlugins() {
            registry.register(createMockPlugin("process-a", "1.0.0")).block();
            registry.register(createMockPlugin("process-b", "1.0.0")).block();
            
            registry.clear();
            
            assertEquals(0, registry.size());
            assertEquals(0, registry.totalVersionCount());
            assertFalse(registry.contains("process-a"));
            assertFalse(registry.contains("process-b"));
        }
    }
}
