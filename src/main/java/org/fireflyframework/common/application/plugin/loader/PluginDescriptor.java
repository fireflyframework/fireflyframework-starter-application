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

package org.fireflyframework.application.plugin.loader;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.net.URI;
import java.util.Map;

/**
 * Descriptor for a plugin to be loaded.
 * 
 * <p>PluginDescriptor contains all the information needed to locate and
 * load a process plugin from any supported source.</p>
 * 
 * <h3>Source Types</h3>
 * <ul>
 *   <li>{@code spring-bean} - Plugin is a Spring bean in the application context</li>
 *   <li>{@code jar} - Plugin is in an external JAR file</li>
 *   <li>{@code remote-maven} - Plugin is in a Maven repository</li>
 *   <li>{@code remote-git} - Plugin source is in a Git repository</li>
 *   <li>{@code remote-http} - Plugin is downloadable from an HTTP URL</li>
 * </ul>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
public class PluginDescriptor {
    
    /**
     * Unique identifier for the process plugin.
     */
    String processId;
    
    /**
     * Version of the plugin.
     */
    String version;
    
    /**
     * The source type for this plugin.
     * Examples: "spring-bean", "jar", "remote-maven", "remote-git"
     */
    String sourceType;
    
    /**
     * URI or path to the plugin source.
     * For JAR: file path; for remote: repository URL
     */
    String sourceUri;
    
    /**
     * For JAR plugins: the fully qualified class name of the plugin.
     */
    String className;
    
    /**
     * For Maven plugins: the group ID.
     */
    String groupId;
    
    /**
     * For Maven plugins: the artifact ID.
     */
    String artifactId;
    
    /**
     * For Git plugins: the branch or tag to use.
     */
    String gitRef;
    
    /**
     * For Git plugins: the path within the repository.
     */
    String gitPath;
    
    /**
     * Checksum for verification (SHA-256).
     */
    String checksum;
    
    /**
     * Additional properties for loader-specific configuration.
     */
    @Singular
    Map<String, Object> properties;
    
    /**
     * Whether to force reload even if already loaded.
     */
    @Builder.Default
    boolean forceReload = false;
    
    /**
     * Creates a descriptor for a Spring bean plugin.
     * 
     * @param processId the process ID
     * @param className the bean class name
     * @return a new PluginDescriptor
     */
    public static PluginDescriptor springBean(String processId, String className) {
        return PluginDescriptor.builder()
                .processId(processId)
                .sourceType("spring-bean")
                .className(className)
                .build();
    }
    
    /**
     * Creates a descriptor for a JAR plugin.
     * 
     * @param processId the process ID
     * @param jarPath the path to the JAR file
     * @param className the plugin class name
     * @return a new PluginDescriptor
     */
    public static PluginDescriptor jar(String processId, String jarPath, String className) {
        return PluginDescriptor.builder()
                .processId(processId)
                .sourceType("jar")
                .sourceUri(jarPath)
                .className(className)
                .build();
    }
    
    /**
     * Creates a descriptor for a Maven plugin.
     * 
     * @param groupId the Maven group ID
     * @param artifactId the Maven artifact ID
     * @param version the version
     * @param className the plugin class name
     * @return a new PluginDescriptor
     */
    public static PluginDescriptor maven(String groupId, String artifactId, String version, String className) {
        return PluginDescriptor.builder()
                .processId(artifactId)
                .sourceType("remote-maven")
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version)
                .className(className)
                .build();
    }
    
    /**
     * Creates a descriptor for a Git repository plugin.
     * 
     * @param processId the process ID
     * @param repoUrl the Git repository URL
     * @param ref the branch/tag/commit reference
     * @param path the path within the repository
     * @return a new PluginDescriptor
     */
    public static PluginDescriptor git(String processId, String repoUrl, String ref, String path) {
        return PluginDescriptor.builder()
                .processId(processId)
                .sourceType("remote-git")
                .sourceUri(repoUrl)
                .gitRef(ref)
                .gitPath(path)
                .build();
    }
    
    /**
     * Creates a descriptor for an HTTP download plugin.
     * 
     * @param processId the process ID
     * @param url the download URL
     * @param className the plugin class name
     * @return a new PluginDescriptor
     */
    public static PluginDescriptor http(String processId, String url, String className) {
        return PluginDescriptor.builder()
                .processId(processId)
                .sourceType("remote-http")
                .sourceUri(url)
                .className(className)
                .build();
    }
    
    /**
     * Checks if this is a Spring bean plugin.
     * 
     * @return true if spring-bean source
     */
    public boolean isSpringBean() {
        return "spring-bean".equals(sourceType);
    }
    
    /**
     * Checks if this is a JAR plugin.
     * 
     * @return true if jar source
     */
    public boolean isJar() {
        return "jar".equals(sourceType);
    }
    
    /**
     * Checks if this is a remote plugin.
     * 
     * @return true if any remote source
     */
    public boolean isRemote() {
        return sourceType != null && sourceType.startsWith("remote-");
    }
    
    /**
     * Gets the source URI as a URI object.
     * 
     * @return the URI, or null if not set
     */
    public URI getSourceAsUri() {
        if (sourceUri == null || sourceUri.isEmpty()) {
            return null;
        }
        return URI.create(sourceUri);
    }
    
    /**
     * Gets a property value.
     * 
     * @param key the property key
     * @param type the expected type
     * @param <T> the type parameter
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
}
