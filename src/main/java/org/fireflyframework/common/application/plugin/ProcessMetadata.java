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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Metadata describing a process plugin, including its identity, capabilities,
 * and requirements.
 * 
 * <p>ProcessMetadata is used for:</p>
 * <ul>
 *   <li>Plugin registration and discovery</li>
 *   <li>Capability-based plugin selection</li>
 *   <li>Permission validation before execution</li>
 *   <li>Documentation and introspection</li>
 * </ul>
 * 
 * <h3>Example</h3>
 * <pre>
 * ProcessMetadata metadata = ProcessMetadata.builder()
 *     .processId("vanilla-account-creation")
 *     .name("Standard Account Creation")
 *     .version("1.0.0")
 *     .description("Creates a new account with standard KYC verification")
 *     .capability("ACCOUNT_CREATION")
 *     .capability("KYC_VERIFICATION")
 *     .requiredPermission("accounts:create")
 *     .requiredPermission("kyc:verify")
 *     .build();
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
public class ProcessMetadata {
    
    /**
     * Unique identifier for the process.
     * Used for registration and configuration mapping.
     */
    String processId;
    
    /**
     * Human-readable name for the process.
     */
    String name;
    
    /**
     * Version of the process (semantic versioning recommended).
     */
    String version;
    
    /**
     * Detailed description of what the process does.
     */
    String description;
    
    /**
     * Category or domain of the process (e.g., "ACCOUNTS", "PAYMENTS", "LOANS").
     */
    String category;
    
    /**
     * Capabilities provided by this process.
     * Used for capability-based discovery and routing.
     */
    @Singular
    Set<String> capabilities;
    
    /**
     * Permissions required to execute this process.
     * Validated by ProcessPluginExecutor before execution.
     */
    @Singular
    Set<String> requiredPermissions;
    
    /**
     * Roles that can execute this process.
     * Alternative to permission-based authorization.
     */
    @Singular
    Set<String> requiredRoles;
    
    /**
     * Feature flags that must be enabled for this process.
     */
    @Singular
    Set<String> requiredFeatures;
    
    /**
     * Tags for categorization and filtering.
     */
    @Singular
    Set<String> tags;
    
    /**
     * Custom properties for extension.
     */
    @Singular
    Map<String, Object> properties;
    
    /**
     * The source/loader type that provided this plugin.
     * Examples: "spring-bean", "jar", "remote-maven", "remote-git"
     */
    String sourceType;
    
    /**
     * URI or path to the source of this plugin.
     * For JAR plugins: file path; for remote: repository URL
     */
    String sourceUri;
    
    /**
     * Indicates if this is a vanilla (default) implementation.
     * Vanilla processes serve as fallbacks when no custom process is configured.
     */
    @Builder.Default
    boolean vanilla = false;
    
    /**
     * Indicates if this process is deprecated.
     * Deprecated processes log warnings when executed.
     */
    @Builder.Default
    boolean deprecated = false;
    
    /**
     * The replacement process ID if this process is deprecated.
     */
    String replacedBy;
    
    /**
     * Expected input class type (for documentation/validation).
     */
    Class<?> inputType;
    
    /**
     * Expected output class type (for documentation/validation).
     */
    Class<?> outputType;
    
    /**
     * Creates a minimal metadata instance with only required fields.
     * 
     * @param processId the process ID
     * @param version the version
     * @return a new ProcessMetadata instance
     */
    public static ProcessMetadata minimal(String processId, String version) {
        return ProcessMetadata.builder()
                .processId(processId)
                .version(version)
                .build();
    }
    
    /**
     * Checks if this process has a specific capability.
     * 
     * @param capability the capability to check
     * @return true if the capability is present
     */
    public boolean hasCapability(String capability) {
        return capabilities != null && capabilities.contains(capability);
    }
    
    /**
     * Checks if this process requires a specific permission.
     * 
     * @param permission the permission to check
     * @return true if the permission is required
     */
    public boolean requiresPermission(String permission) {
        return requiredPermissions != null && requiredPermissions.contains(permission);
    }
    
    /**
     * Checks if any permissions are required.
     * 
     * @return true if permissions are required
     */
    public boolean hasRequiredPermissions() {
        return requiredPermissions != null && !requiredPermissions.isEmpty();
    }
    
    /**
     * Checks if any roles are required.
     * 
     * @return true if roles are required
     */
    public boolean hasRequiredRoles() {
        return requiredRoles != null && !requiredRoles.isEmpty();
    }
}
