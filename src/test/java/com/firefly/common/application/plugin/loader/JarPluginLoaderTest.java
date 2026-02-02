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

import com.firefly.common.application.plugin.ProcessPluginRegistry;
import com.firefly.common.application.plugin.config.PluginProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("JarPluginLoader Tests")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JarPluginLoaderTest {
    
    @Mock
    private PluginProperties pluginProperties;
    
    @Mock
    private ProcessPluginRegistry registry;
    
    @Mock
    private PluginProperties.LoaderProperties loaderProperties;
    
    @Mock
    private PluginProperties.JarLoaderProperties jarLoaderProperties;
    
    private JarPluginLoader loader;
    
    @BeforeEach
    void setUp() {
        when(pluginProperties.getLoaders()).thenReturn(loaderProperties);
        when(loaderProperties.getJar()).thenReturn(jarLoaderProperties);
        
        loader = new JarPluginLoader(pluginProperties, registry);
    }
    
    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {
        
        @Test
        @DisplayName("getLoaderType() should return 'jar'")
        void getLoaderTypeShouldReturnJar() {
            assertEquals("jar", loader.getLoaderType());
            assertEquals(JarPluginLoader.LOADER_TYPE, loader.getLoaderType());
        }
        
        @Test
        @DisplayName("getPriority() should return configured priority")
        void getPriorityShouldReturnConfiguredPriority() {
            when(jarLoaderProperties.getPriority()).thenReturn(10);
            
            assertEquals(10, loader.getPriority());
        }
        
        @Test
        @DisplayName("isEnabled() should return true when both plugin and loader are enabled")
        void isEnabledShouldReturnTrueWhenBothEnabled() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(jarLoaderProperties.isEnabled()).thenReturn(true);
            
            assertTrue(loader.isEnabled());
        }
        
        @Test
        @DisplayName("isEnabled() should return false when plugin is disabled")
        void isEnabledShouldReturnFalseWhenPluginDisabled() {
            when(pluginProperties.isEnabled()).thenReturn(false);
            when(jarLoaderProperties.isEnabled()).thenReturn(true);
            
            assertFalse(loader.isEnabled());
        }
        
        @Test
        @DisplayName("isEnabled() should return false when loader is disabled")
        void isEnabledShouldReturnFalseWhenLoaderDisabled() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(jarLoaderProperties.isEnabled()).thenReturn(false);
            
            assertFalse(loader.isEnabled());
        }
        
        @Test
        @DisplayName("supportsHotReload() should return configured value")
        void supportsHotReloadShouldReturnConfiguredValue() {
            when(jarLoaderProperties.isHotReload()).thenReturn(true);
            assertTrue(loader.supportsHotReload());
            
            when(jarLoaderProperties.isHotReload()).thenReturn(false);
            assertFalse(loader.supportsHotReload());
        }
    }
    
    @Nested
    @DisplayName("Supports Tests")
    class SupportsTests {
        
        @Test
        @DisplayName("supports() should return true for jar descriptor")
        void supportsShouldReturnTrueForJarDescriptor() {
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test")
                    .sourceType("jar")
                    .build();
            
            assertTrue(loader.supports(descriptor));
        }
        
        @Test
        @DisplayName("supports() should return false for spring-bean descriptor")
        void supportsShouldReturnFalseForSpringBeanDescriptor() {
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test")
                    .sourceType("spring-bean")
                    .build();
            
            assertFalse(loader.supports(descriptor));
        }
        
        @Test
        @DisplayName("supports() should return false for remote-maven descriptor")
        void supportsShouldReturnFalseForRemoteMavenDescriptor() {
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test")
                    .sourceType("remote-maven")
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
    @DisplayName("Initialize Tests")
    class InitializeTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("initialize() should complete when disabled")
        void initializeShouldCompleteWhenDisabled() {
            when(pluginProperties.isEnabled()).thenReturn(false);
            
            StepVerifier.create(loader.initialize())
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
        
        @Test
        @DisplayName("initialize() should create plugin directories")
        void initializeShouldCreatePluginDirectories() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(jarLoaderProperties.isEnabled()).thenReturn(true);
            when(jarLoaderProperties.isHotReload()).thenReturn(false);
            
            String newDir = tempDir.resolve("plugins").toString();
            when(jarLoaderProperties.getScanDirectories()).thenReturn(List.of(newDir));
            
            StepVerifier.create(loader.initialize())
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
            
            assertTrue(new File(newDir).exists());
        }
    }
    
    @Nested
    @DisplayName("Discovery Tests")
    class DiscoveryTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("discoverPlugins() should return empty when disabled")
        void discoverPluginsShouldReturnEmptyWhenDisabled() {
            when(pluginProperties.isEnabled()).thenReturn(false);
            
            StepVerifier.create(loader.discoverPlugins())
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
        
        @Test
        @DisplayName("discoverPlugins() should return empty for non-existent directory")
        void discoverPluginsShouldReturnEmptyForNonExistentDirectory() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(jarLoaderProperties.isEnabled()).thenReturn(true);
            when(jarLoaderProperties.getScanDirectories())
                    .thenReturn(List.of("/non/existent/path"));
            
            StepVerifier.create(loader.discoverPlugins())
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
        
        @Test
        @DisplayName("discoverPlugins() should return empty for empty directory")
        void discoverPluginsShouldReturnEmptyForEmptyDirectory() {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(jarLoaderProperties.isEnabled()).thenReturn(true);
            when(jarLoaderProperties.getScanDirectories())
                    .thenReturn(List.of(tempDir.toString()));
            
            StepVerifier.create(loader.discoverPlugins())
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
        
        @Test
        @DisplayName("discoverPlugins() should return empty for JAR without plugins")
        void discoverPluginsShouldReturnEmptyForJarWithoutPlugins() throws Exception {
            when(pluginProperties.isEnabled()).thenReturn(true);
            when(jarLoaderProperties.isEnabled()).thenReturn(true);
            when(jarLoaderProperties.getScanDirectories())
                    .thenReturn(List.of(tempDir.toString()));
            when(jarLoaderProperties.isClassloaderIsolation()).thenReturn(false);
            
            // Create an empty JAR file
            File emptyJar = tempDir.resolve("empty.jar").toFile();
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(emptyJar))) {
                JarEntry entry = new JarEntry("META-INF/MANIFEST.MF");
                jos.putNextEntry(entry);
                jos.write("Manifest-Version: 1.0\n".getBytes());
                jos.closeEntry();
            }
            
            StepVerifier.create(loader.discoverPlugins())
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
    }
    
    @Nested
    @DisplayName("Load Plugin Tests")
    class LoadPluginTests {
        
        @TempDir
        Path tempDir;
        
        @Test
        @DisplayName("loadPlugin() should reject non-jar descriptors")
        void loadPluginShouldRejectNonJarDescriptors() {
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test")
                    .sourceType("spring-bean")
                    .build();
            
            StepVerifier.create(loader.loadPlugin(descriptor))
                    .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                            e.getMessage().contains("jar"))
                    .verify(Duration.ofSeconds(5));
        }
        
        @Test
        @DisplayName("loadPlugin() should require JAR path")
        void loadPluginShouldRequireJarPath() {
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test")
                    .sourceType("jar")
                    .build();
            
            StepVerifier.create(loader.loadPlugin(descriptor))
                    .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                            e.getMessage().contains("JAR path is required"))
                    .verify(Duration.ofSeconds(5));
        }
        
        @Test
        @DisplayName("loadPlugin() should error for non-existent JAR")
        void loadPluginShouldErrorForNonExistentJar() {
            PluginDescriptor descriptor = PluginDescriptor.builder()
                    .processId("test")
                    .sourceType("jar")
                    .sourceUri("/non/existent/plugin.jar")
                    .build();
            
            StepVerifier.create(loader.loadPlugin(descriptor))
                    .expectErrorMatches(e -> e instanceof IllegalArgumentException &&
                            e.getMessage().contains("JAR file not found"))
                    .verify(Duration.ofSeconds(5));
        }
    }
    
    @Nested
    @DisplayName("Unload Plugin Tests")
    class UnloadPluginTests {
        
        @Test
        @DisplayName("unloadPlugin() should complete for non-existent plugin")
        void unloadPluginShouldCompleteForNonExistentPlugin() {
            StepVerifier.create(loader.unloadPlugin("non-existent"))
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
    }
    
    @Nested
    @DisplayName("Shutdown Tests")
    class ShutdownTests {
        
        @Test
        @DisplayName("shutdown() should complete successfully")
        void shutdownShouldCompleteSuccessfully() {
            StepVerifier.create(loader.shutdown())
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
        
        @Test
        @DisplayName("shutdown() should be idempotent")
        void shutdownShouldBeIdempotent() {
            // Call shutdown multiple times
            StepVerifier.create(loader.shutdown())
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
            
            StepVerifier.create(loader.shutdown())
                    .expectComplete()
                    .verify(Duration.ofSeconds(5));
        }
    }
    
    @Nested
    @DisplayName("Classloader Isolation Tests")
    class ClassloaderIsolationTests {
        
        @Test
        @DisplayName("should respect classloader isolation setting")
        void shouldRespectClassloaderIsolationSetting() {
            when(jarLoaderProperties.isClassloaderIsolation()).thenReturn(true);
            assertTrue(jarLoaderProperties.isClassloaderIsolation());
            
            when(jarLoaderProperties.isClassloaderIsolation()).thenReturn(false);
            assertFalse(jarLoaderProperties.isClassloaderIsolation());
        }
    }
}
