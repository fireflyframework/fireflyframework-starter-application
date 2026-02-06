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

package org.fireflyframework.application.plugin.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Firefly Process Plugin.
 * 
 * <p>Classes annotated with {@code @FireflyProcess} are automatically
 * discovered and registered in the {@code ProcessPluginRegistry} by
 * the {@code SpringBeanPluginLoader}.</p>
 * 
 * <p>The annotated class must implement the {@code ProcessPlugin} interface.</p>
 * 
 * <h3>Usage Example</h3>
 * <pre>
 * &#64;FireflyProcess(
 *     id = "vanilla-account-creation",
 *     name = "Standard Account Creation",
 *     version = "1.0.0",
 *     description = "Creates accounts with standard KYC verification",
 *     capabilities = {"ACCOUNT_CREATION", "KYC_VERIFICATION"},
 *     requiredPermissions = {"accounts:create", "kyc:verify"}
 * )
 * public class VanillaAccountCreationProcess implements ProcessPlugin {
 *     
 *     &#64;Override
 *     public Mono&lt;ProcessResult&gt; execute(ProcessExecutionContext context) {
 *         // Process implementation
 *     }
 * }
 * </pre>
 * 
 * <h3>Metadata Generation</h3>
 * <p>The annotation attributes are used to generate {@code ProcessMetadata}
 * for the plugin. Additional metadata can be provided by overriding
 * {@code ProcessPlugin.getMetadata()} in the implementation.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see org.fireflyframework.application.plugin.ProcessPlugin
 * @see org.fireflyframework.application.plugin.ProcessMetadata
 * @see org.fireflyframework.application.plugin.ProcessPluginRegistry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface FireflyProcess {
    
    /**
     * Unique identifier for the process.
     * 
     * <p>This ID is used for:</p>
     * <ul>
     *   <li>Registration in the plugin registry</li>
     *   <li>API-to-process mapping configuration</li>
     *   <li>Audit logging and tracing</li>
     * </ul>
     * 
     * <p>Convention: use kebab-case (e.g., "vanilla-account-creation")</p>
     * 
     * @return the unique process identifier
     */
    String id();
    
    /**
     * Human-readable name for the process.
     * 
     * @return the display name
     */
    String name() default "";
    
    /**
     * Version of the process (semantic versioning recommended).
     * 
     * <p>Multiple versions of the same process ID can coexist,
     * allowing gradual rollout and tenant-specific pinning.</p>
     * 
     * @return the version string
     */
    String version() default "1.0.0";
    
    /**
     * Detailed description of what the process does.
     * 
     * @return the description
     */
    String description() default "";
    
    /**
     * Category or domain of the process.
     * 
     * <p>Examples: "ACCOUNTS", "PAYMENTS", "LOANS", "KYC"</p>
     * 
     * @return the category
     */
    String category() default "";
    
    /**
     * Capabilities provided by this process.
     * 
     * <p>Used for capability-based routing and discovery.
     * For example, if multiple processes provide "ACCOUNT_CREATION",
     * the system can route based on additional criteria.</p>
     * 
     * @return array of capabilities
     */
    String[] capabilities() default {};
    
    /**
     * Permissions required to execute this process.
     * 
     * <p>These are validated against the calling party's permissions
     * before process execution. All listed permissions must be present.</p>
     * 
     * @return array of required permission strings
     */
    String[] requiredPermissions() default {};
    
    /**
     * Roles that can execute this process.
     * 
     * <p>Alternative to permission-based authorization.
     * At least one of the listed roles must be present.</p>
     * 
     * @return array of required role strings
     */
    String[] requiredRoles() default {};
    
    /**
     * Feature flags that must be enabled for this process to be available.
     * 
     * <p>If any required feature is disabled for the tenant,
     * the process will not be available.</p>
     * 
     * @return array of required feature flag names
     */
    String[] requiredFeatures() default {};
    
    /**
     * Tags for categorization and filtering.
     * 
     * @return array of tags
     */
    String[] tags() default {};
    
    /**
     * Whether this is a vanilla (default) implementation.
     * 
     * <p>Vanilla processes serve as fallbacks when no custom
     * process is configured for a tenant/product.</p>
     * 
     * @return true if this is a vanilla implementation
     */
    boolean vanilla() default false;
    
    /**
     * Whether this process is deprecated.
     * 
     * <p>Deprecated processes log warnings when executed and
     * should include a {@code replacedBy} value.</p>
     * 
     * @return true if deprecated
     */
    boolean deprecated() default false;
    
    /**
     * The replacement process ID if this process is deprecated.
     * 
     * @return the replacement process ID
     */
    String replacedBy() default "";
}
