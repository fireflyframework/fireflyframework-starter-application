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

import reactor.core.publisher.Mono;

/**
 * Core interface for pluggable business processes in the Firefly Banking Platform.
 * 
 * <p>ProcessPlugins are the fundamental building blocks of the BaaS API layer.
 * They encapsulate business logic that can be dynamically resolved and executed
 * at runtime based on tenant, product, and channel configuration.</p>
 * 
 * <h3>Plugin Lifecycle</h3>
 * <ol>
 *   <li><b>Registration</b> - Plugin is discovered and registered with {@code ProcessPluginRegistry}</li>
 *   <li><b>Resolution</b> - API request triggers process resolution via configuration</li>
 *   <li><b>Validation</b> - Optional pre-execution validation of inputs</li>
 *   <li><b>Execution</b> - Process executes with full application context</li>
 *   <li><b>Compensation</b> - Optional rollback on failure (saga pattern)</li>
 * </ol>
 * 
 * <h3>Implementation Example</h3>
 * <pre>
 * &#64;FireflyProcess(
 *     id = "vanilla-account-creation",
 *     name = "Standard Account Creation",
 *     version = "1.0.0",
 *     capabilities = {"ACCOUNT_CREATION"},
 *     requiredPermissions = {"accounts:create"}
 * )
 * public class VanillaAccountCreationProcess implements ProcessPlugin {
 *     
 *     private final AccountDomainService accountService;
 *     
 *     &#64;Override
 *     public Mono&lt;ProcessResult&gt; execute(ProcessExecutionContext context) {
 *         AccountCreationRequest request = context.getInput(AccountCreationRequest.class);
 *         return accountService.createAccount(context.getAppContext(), request)
 *             .map(ProcessResult::success);
 *     }
 * }
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see ProcessPluginRegistry
 * @see ProcessExecutionContext
 * @see ProcessResult
 */
public interface ProcessPlugin {
    
    /**
     * Returns the unique identifier for this process plugin.
     * 
     * <p>The process ID is used for:</p>
     * <ul>
     *   <li>Registration in the plugin registry</li>
     *   <li>Configuration mapping (API-to-process)</li>
     *   <li>Audit logging and tracing</li>
     * </ul>
     * 
     * @return the unique process identifier
     */
    String getProcessId();
    
    /**
     * Returns the version of this process plugin.
     * 
     * <p>Versioning enables:</p>
     * <ul>
     *   <li>Multiple versions of the same process to coexist</li>
     *   <li>Tenant-specific version pinning</li>
     *   <li>Gradual rollout of process updates</li>
     * </ul>
     * 
     * @return the version string (e.g., "1.0.0")
     */
    String getVersion();
    
    /**
     * Returns the metadata for this process plugin.
     * 
     * <p>Metadata includes descriptive information and configuration
     * requirements such as name, description, capabilities, and
     * required permissions.</p>
     * 
     * @return the process metadata
     */
    ProcessMetadata getMetadata();
    
    /**
     * Executes the business process with the given execution context.
     * 
     * <p>This is the main entry point for process execution. The context
     * contains the full {@code ApplicationExecutionContext} including
     * security, configuration, and business context, plus the operation
     * inputs.</p>
     * 
     * <p><b>Important:</b> Implementations should be non-blocking and
     * return reactive types. Long-running operations should be offloaded
     * appropriately.</p>
     * 
     * @param context the process execution context
     * @return a Mono emitting the process result
     */
    Mono<ProcessResult> execute(ProcessExecutionContext context);
    
    /**
     * Validates the execution context and inputs before execution.
     * 
     * <p>This method is called before {@code execute()} to perform
     * input validation. Default implementation returns an empty
     * validation result (valid).</p>
     * 
     * @param context the process execution context
     * @return a Mono emitting the validation result
     */
    default Mono<ValidationResult> validate(ProcessExecutionContext context) {
        return Mono.just(ValidationResult.valid());
    }
    
    /**
     * Compensates/rollbacks a previously executed process.
     * 
     * <p>This method supports the saga pattern for distributed transactions.
     * It is called when a downstream process fails and previous processes
     * need to be rolled back.</p>
     * 
     * <p>Default implementation returns success with compensated=true.</p>
     * 
     * @param context the process execution context (with original inputs)
     * @return a Mono emitting the compensation result
     */
    default Mono<ProcessResult> compensate(ProcessExecutionContext context) {
        return Mono.just(ProcessResult.success(java.util.Map.of("compensated", true)));
    }
    
    /**
     * Called when the plugin is being initialized.
     * 
     * <p>Override this method to perform any setup required before
     * the plugin can handle requests.</p>
     * 
     * @return a Mono that completes when initialization is done
     */
    default Mono<Void> onInit() {
        return Mono.empty();
    }
    
    /**
     * Called when the plugin is being destroyed/unloaded.
     * 
     * <p>Override this method to perform cleanup such as closing
     * connections or releasing resources.</p>
     * 
     * @return a Mono that completes when cleanup is done
     */
    default Mono<Void> onDestroy() {
        return Mono.empty();
    }
    
    /**
     * Performs a health check on this plugin.
     * 
     * <p>Override this method to implement custom health checks that verify
     * the plugin's dependencies and operational status.</p>
     * 
     * <p>Default implementation returns UP status. Plugins that depend on
     * external services (databases, APIs, etc.) should override this to
     * verify connectivity.</p>
     * 
     * <h3>Implementation Example</h3>
     * <pre>
     * &#64;Override
     * public Mono&lt;HealthStatus&gt; healthCheck() {
     *     return externalService.ping()
     *         .map(r -> HealthStatus.up())
     *         .onErrorReturn(HealthStatus.down("External service unavailable"));
     * }
     * </pre>
     * 
     * @return a Mono emitting the health status
     * @since 1.0.0
     */
    default Mono<HealthStatus> healthCheck() {
        return Mono.just(HealthStatus.up());
    }
}
