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

package com.firefly.common.application.plugin.loader;

import com.firefly.common.application.plugin.*;
import com.firefly.common.application.plugin.annotation.FireflyProcess;
import com.firefly.common.application.plugin.config.PluginProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SpringBeanPluginLoader Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpringBeanPluginLoaderTest {
    
    @Mock
    private PluginProperties pluginProperties;
    
    @Mock
    private ApplicationContext applicationContext;
    
    @Mock
    private PluginProperties.LoaderProperties loaderProperties;
    
    @Mock
    private PluginProperties.SpringBeanLoaderProperties springBeanLoaderProperties;
    
    private SpringBeanPluginLoader loader;
    
    @BeforeEach
    void setUp() {
        when(pluginProperties.getLoaders()).thenReturn(loaderProperties);
        when(loaderProperties.getSpringBean()).thenReturn(springBeanLoaderProperties);
        
        loader = new SpringBeanPluginLoader(pluginProperties);
        loader.setApplicationContext(applicationContext);
    }
    
    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {
        
        @Test
        @DisplayName("getLoaderType() should return 'spring-bean'")
        void getLoaderTypeShouldReturnSpringBean() {
            assertEquals("spring-bean", loader.getLoaderType());
            assertEquals(SpringBeanPluginLoader.LOADER_TYPE, loader.getLoaderType());
        }
        
        @Test
        @DisplayName("getPriority() should return configured priority")
        void getPriorityShouldReturnConfiguredPriority() {
            when(springBeanLoaderProperties.getPriority()).thenReturn(0);
            
            assertEquals(0, loader.getPriority());
        }
        
        @Test
        @DisplayName("isEnabled() should return true when both plugin and loader are enabled")
        void isEnabledShouldReturnTrueWhenBothEnabled() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(springBeanLoaderProperties.isEnabled()).thenReturn(true);
            
            assertTrue(loader.isEnabled());
        }
        
        @Test
        @DisplayName("isEnabled() should return false when plugin is disabled")
        void isEnabledShouldReturnFalseWhenPluginDisabled() {
            when(pluginProperties.isEnabled()).thenReturn(false);
            when(springBeanLoaderProperties.isEnabled()).thenReturn(true);
            
            assertFalse(loader.isEnabled());
        }
        
        @Test
        @DisplayName("isEnabled() should return false when loader is disabled")
        void isEnabledShouldReturnFalseWhenLoaderDisabled() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(springBeanLoaderProperties.isEnabled()).thenReturn(false);
            
            assertFalse(loader.isEnabled());
        }
    }
    
    @Nested
    @DisplayName("Supports Tests")
    class SupportsTests {
        
        @Test
        @DisplayName("supports() should return true for spring-bean descriptor")
        void supportsShouldReturnTrueForSpringBeanDescriptor() {
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test")
                    .sourceType("spring-bean")
                    .build();
            
            assertTrue(loader.supports(descriptor));
        }
        
        @Test
        @DisplayName("supports() should return false for jar descriptor")
        void supportsShouldReturnFalseForJarDescriptor() {
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test")
                    .sourceType("jar")
                    .build();
            
            assertFalse(loader.supports(descriptor));
        }
        
        @Test
        @DisplayName("supports() should return false for null descriptor")
        void supportsShouldReturnFalseForNullDescriptor() {
            assertFalse(loader.supports(null));
        }
    }
    
    @Nested
    @DisplayName("Discovery Tests")
    class DiscoveryTests {
        
        @Test
        @DisplayName("discoverPlugins() should return empty when disabled")
        void discoverPluginsShouldReturnEmptyWhenDisabled() {
            when(pluginProperties.isEnabled()).thenReturn(false);
            
            StepVerifier.create(loader.discoverPlugins())
                    .expectComplete()
                    .verify();
        }
        
        @Test
        @DisplayName("discoverPlugins() should discover annotated ProcessPlugin beans")
        void discoverPluginsShouldDiscoverAnnotatedBeans() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(springBeanLoaderProperties.isEnabled()).thenReturn(true);
            
            // Create a mock plugin with annotation
            TestProcessPlugin testPlugin = new TestProcessPlugin();
            Map<String, Object> annotatedBeans = new HashMap<>();
            annotatedBeans.put("testPlugin", testPlugin);
            
            when(applicationContext.getBeansWithAnnotation(FireflyProcess.class))
                    .thenReturn(annotatedBeans);
            
            StepVerifier.create(loader.discoverPlugins())
                    .assertNext(plugin -> {
                        assertEquals("test-process", plugin.getProcessId());
                        assertEquals("1.0.0", plugin.getVersion());
                    })
                    .expectComplete()
                    .verify();
        }
        
        @Test
        @DisplayName("discoverPlugins() should skip beans that don't implement ProcessPlugin")
        void discoverPluginsShouldSkipNonProcessPluginBeans() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(springBeanLoaderProperties.isEnabled()).thenReturn(true);
            
            // Create a mock non-plugin bean with the annotation
            Object nonPlugin = new Object();
            Map<String, Object> annotatedBeans = new HashMap<>();
            annotatedBeans.put("nonPlugin", nonPlugin);
            
            when(applicationContext.getBeansWithAnnotation(FireflyProcess.class))
                    .thenReturn(annotatedBeans);
            
            StepVerifier.create(loader.discoverPlugins())
                    .expectComplete()
                    .verify();
        }
    }
    
    @Nested
    @DisplayName("Load Plugin Tests")
    class LoadPluginTests {
        
        @Test
        @DisplayName("loadPlugin() should reject non-spring-bean descriptors")
        void loadPluginShouldRejectNonSpringBeanDescriptors() {
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test")
                    .sourceType("jar")
                    .build();
            
            StepVerifier.create(loader.loadPlugin(descriptor))
                    .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                            e.getMessage().contains("spring-bean"))
                    .verify();
        }
        
        @Test
        @DisplayName("loadPlugin() by className should load plugin")
        void loadPluginByClassNameShouldLoadPlugin() {
            TestProcessPlugin testPlugin = new TestProcessPlugin();
            
            when(applicationContext.getBean(TestProcessPlugin.class)).thenReturn(testPlugin);
            
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test-process")
                    .sourceType("spring-bean")
                    .className(TestProcessPlugin.class.getName())
                    .build();
            
            StepVerifier.create(loader.loadPlugin(descriptor))
                    .assertNext(plugin -> assertEquals("test-process", plugin.getProcessId()))
                    .expectComplete()
                    .verify();
        }
        
        @Test
        @DisplayName("loadPlugin() by processId should find and load plugin")
        void loadPluginByProcessIdShouldFindAndLoadPlugin() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(springBeanLoaderProperties.isEnabled()).thenReturn(true);
            
            TestProcessPlugin testPlugin = new TestProcessPlugin();
            Map<String, Object> annotatedBeans = new HashMap<>();
            annotatedBeans.put("testPlugin", testPlugin);
            
            when(applicationContext.getBeansWithAnnotation(FireflyProcess.class))
                    .thenReturn(annotatedBeans);
            
            // First discover to populate cache
            loader.discoverPlugins().collectList().block();
            
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test-process")
                    .sourceType("spring-bean")
                    .build();
            
            StepVerifier.create(loader.loadPlugin(descriptor))
                    .assertNext(plugin -> assertEquals("test-process", plugin.getProcessId()))
                    .expectComplete()
                    .verify();
        }
    }
    
    @Nested
    @DisplayName("Unload Plugin Tests")
    class UnloadPluginTests {
        
        @Test
        @DisplayName("unloadPlugin() should remove plugin from cache")
        void unloadPluginShouldRemoveFromCache() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(springBeanLoaderProperties.isEnabled()).thenReturn(true);
            
            TestProcessPlugin testPlugin = new TestProcessPlugin();
            Map<String, Object> annotatedBeans = new HashMap<>();
            annotatedBeans.put("testPlugin", testPlugin);
            
            when(applicationContext.getBeansWithAnnotation(FireflyProcess.class))
                    .thenReturn(annotatedBeans);
            
            // First discover
            loader.discoverPlugins().collectList().block();
            
            // Then unload
            StepVerifier.create(loader.unloadPlugin("test-process"))
                    .expectComplete()
                    .verify();
        }
    }
    
    // Test implementation of ProcessPlugin with @FireflyProcess annotation
    @FireflyProcess(
            id = "test-process",
            version = "1.0.0",
            name = "Test Process",
            description = "A test process plugin",
            capabilities = {"TEST_CAPABILITY"},
            requiredPermissions = {"test:execute"}
    )
    static class TestProcessPlugin implements ProcessPlugin {
        
        @Override
        public String getProcessId() {
            return "test-process";
        }
        
        @Override
        public String getVersion() {
            return "1.0.0";
        }
        
        @Override
        public ProcessMetadata getMetadata() {
            return null; // Return null to test annotation-based metadata
        }
        
        @Override
        public Mono<ProcessResult> execute(ProcessExecutionContext context) {
            return Mono.just(ProcessResult.success("test output"));
        }
        
        @Override
        public Mono<ValidationResult> validate(ProcessExecutionContext context) {
            return Mono.just(ValidationResult.valid());
        }
    }
}
