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

package org.fireflyframework.application.security;

import org.fireflyframework.application.security.EndpointSecurityRegistry.EndpointSecurity;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

/**
 * <h1>Abstract Base Class for Declarative Endpoint Security Configuration</h1>
 * 
 * <p>This abstract class simplifies the process of configuring endpoint security using
 * the {@link EndpointSecurityRegistry}. Instead of manually calling registry methods,
 * you can extend this class and override {@link #configureEndpointSecurity()} to
 * define security rules in a clean, declarative way.</p>
 * 
 * <h2>Why Use This?</h2>
 * <ul>
 *   <li><strong>Cleaner code:</strong> Declarative API vs manual registry calls</li>
 *   <li><strong>Type safety:</strong> Builder pattern ensures correct configuration</li>
 *   <li><strong>Readability:</strong> Security rules are clear and organized</li>
 *   <li><strong>Flexibility:</strong> Override annotations dynamically based on environment</li>
 * </ul>
 * 
 * <h2>Quick Example</h2>
 * <pre>
 * {@code
 * @Configuration
 * public class AccountSecurityConfig extends AbstractSecurityConfiguration {
 *     
 *     @Override
 *     protected void configureEndpointSecurity() {
 *         // Simple role-based access
 *         protect("/api/v1/contracts/{contractId}/accounts")
 *             .onMethod("GET")
 *             .requireRoles("ACCOUNT_VIEWER")
 *             .register();
 *         
 *         // Role + Permission
 *         protect("/api/v1/contracts/{contractId}/accounts")
 *             .onMethod("POST")
 *             .requireRoles("ACCOUNT_CREATOR")
 *             .requirePermissions("CREATE_ACCOUNT")
 *             .register();
 *         
 *         // Multiple roles (user needs ALL of them)
 *         protect("/api/v1/contracts/{contractId}/accounts/{accountId}")
 *             .onMethod("DELETE")
 *             .requireAllRoles("ACCOUNT_ADMIN", "DELETE_AUTHORIZED")
 *             .register();
 *         
 *         // Public endpoint
 *         protect("/api/v1/public/rates")
 *             .onMethod("GET")
 *             .allowAnonymous()
 *             .register();
 *     }
 * }
 * }
 * </pre>
 * 
 * <h2>Complete Example with Feature Flags</h2>
 * <pre>
 * {@code
 * @Configuration
 * public class TransactionSecurityConfig extends AbstractSecurityConfiguration {
 *     
 *     @Value("${security.strict-mode:false}")
 *     private boolean strictMode;
 *     
 *     @Override
 *     protected void configureEndpointSecurity() {
 *         if (strictMode) {
 *             // Production: Strict security
 *             protect("/api/v1/contracts/{contractId}/products/{productId}/transactions")
 *                 .onMethod("POST")
 *                 .requireRoles("ACCOUNT_HOLDER")
 *                 .requireAllPermissions("TRANSFER_FUNDS", "HIGH_VALUE_TRANSFER")
 *                 .register();
 *         } else {
 *             // Development: Relaxed security
 *             protect("/api/v1/contracts/{contractId}/products/{productId}/transactions")
 *                 .onMethod("POST")
 *                 .requireRoles("ACCOUNT_HOLDER")
 *                 .requirePermissions("TRANSFER_FUNDS")
 *                 .register();
 *         }
 *     }
 * }
 * }
 * </pre>
 * 
 * <h2>How It Works</h2>
 * <ol>
 *   <li>Extend this class in your {@code @Configuration} class</li>
 *   <li>Override {@link #configureEndpointSecurity()}</li>
 *   <li>Use the {@link #protect(String)} method to start building security rules</li>
 *   <li>Chain methods to configure roles, permissions, authentication requirements</li>
 *   <li>Call {@link EndpointProtectionBuilder#register()} to register the rule</li>
 * </ol>
 * 
 * <h2>Priority</h2>
 * <p>Remember: Configuration defined here (via {@link EndpointSecurityRegistry})
 * <strong>ALWAYS overrides</strong> {@code @Secure} annotations on controller methods.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see EndpointSecurityRegistry
 * @see org.fireflyframework.application.security.annotation.Secure
 */
@Slf4j
public abstract class AbstractSecurityConfiguration {
    
    @Autowired
    private EndpointSecurityRegistry securityRegistry;
    
    /**
     * Override this method to define your endpoint security rules.
     * 
     * <p>This method is automatically called after the Spring context is initialized
     * ({@code @PostConstruct}). Use the {@link #protect(String)} method to start
     * defining security rules for your endpoints.</p>
     * 
     * <p><strong>Example:</strong></p>
     * <pre>
     * {@code
     * @Override
     * protected void configureEndpointSecurity() {
     *     protect("/api/v1/accounts")
     *         .onMethod("POST")
     *         .requireRoles("ACCOUNT_CREATOR")
     *         .register();
     * }
     * }
     * </pre>
     */
    protected abstract void configureEndpointSecurity();
    
    /**
     * Initializes security configuration.
     * 
     * <p>This method is called automatically by Spring after the bean is constructed.
     * It calls {@link #configureEndpointSecurity()} to allow subclasses to define
     * their security rules.</p>
     */
    @PostConstruct
    private void initialize() {
        log.info("Initializing endpoint security configuration: {}", getClass().getSimpleName());
        configureEndpointSecurity();
        log.info("Endpoint security configuration completed: {} endpoints registered", 
                securityRegistry.getAllEndpoints().size());
    }
    
    /**
     * Starts building a security rule for the given endpoint path.
     * 
     * <p>This is the entry point for defining endpoint security. Chain additional
     * methods to configure the security requirements.</p>
     * 
     * <p><strong>Example:</strong></p>
     * <pre>
     * {@code
     * protect("/api/v1/contracts/{contractId}/accounts")
     *     .onMethod("POST")
     *     .requireRoles("ACCOUNT_CREATOR")
     *     .register();
     * }
     * </pre>
     * 
     * @param endpointPath the endpoint path (with path variables like {@code {contractId}})
     * @return a builder to continue configuring the security rule
     */
    protected final EndpointProtectionBuilder protect(String endpointPath) {
        return new EndpointProtectionBuilder(endpointPath, securityRegistry);
    }
    
    /**
     * <h2>Fluent Builder for Endpoint Security Configuration</h2>
     * 
     * <p>This builder provides a clean, fluent API for configuring endpoint security.
     * Chain methods to define the security requirements, then call {@link #register()}
     * to register the configuration.</p>
     * 
     * <h3>Available Methods</h3>
     * <ul>
     *   <li>{@link #onMethod(String)} - Set the HTTP method (GET, POST, PUT, DELETE, etc.)</li>
     *   <li>{@link #requireRoles(String...)} - Require ANY of the specified roles</li>
     *   <li>{@link #requireAllRoles(String...)} - Require ALL of the specified roles</li>
     *   <li>{@link #requirePermissions(String...)} - Require ANY of the specified permissions</li>
     *   <li>{@link #requireAllPermissions(String...)} - Require ALL of the specified permissions</li>
     *   <li>{@link #requireAuthentication()} - Require user to be authenticated (default)</li>
     *   <li>{@link #allowAnonymous()} - Allow unauthenticated access</li>
     *   <li>{@link #register()} - Register the security configuration</li>
     * </ul>
     */
    protected static final class EndpointProtectionBuilder {
        private final String endpointPath;
        private final EndpointSecurityRegistry registry;
        
        private String httpMethod = "GET";
        private Set<String> roles = Set.of();
        private Set<String> permissions = Set.of();
        private boolean requireAllRoles = false;
        private boolean requireAllPermissions = false;
        private boolean allowAnonymous = false;
        private boolean requiresAuthentication = true;
        
        private EndpointProtectionBuilder(String endpointPath, EndpointSecurityRegistry registry) {
            this.endpointPath = endpointPath;
            this.registry = registry;
        }
        
        /**
         * Sets the HTTP method for this security rule.
         * 
         * @param method the HTTP method (e.g., "GET", "POST", "PUT", "DELETE")
         * @return this builder for method chaining
         */
        public EndpointProtectionBuilder onMethod(String method) {
            this.httpMethod = method.toUpperCase();
            return this;
        }
        
        /**
         * Requires the user to have ANY of the specified roles.
         * 
         * <p>The user needs at least one of these roles to access the endpoint.</p>
         * 
         * @param roles the required roles
         * @return this builder for method chaining
         */
        public EndpointProtectionBuilder requireRoles(String... roles) {
            this.roles = Set.of(roles);
            this.requireAllRoles = false;
            return this;
        }
        
        /**
         * Requires the user to have ALL of the specified roles.
         * 
         * <p>The user must have every single one of these roles to access the endpoint.</p>
         * 
         * @param roles the required roles
         * @return this builder for method chaining
         */
        public EndpointProtectionBuilder requireAllRoles(String... roles) {
            this.roles = Set.of(roles);
            this.requireAllRoles = true;
            return this;
        }
        
        /**
         * Requires the user to have ANY of the specified permissions.
         * 
         * <p>The user needs at least one of these permissions to access the endpoint.</p>
         * 
         * @param permissions the required permissions
         * @return this builder for method chaining
         */
        public EndpointProtectionBuilder requirePermissions(String... permissions) {
            this.permissions = Set.of(permissions);
            this.requireAllPermissions = false;
            return this;
        }
        
        /**
         * Requires the user to have ALL of the specified permissions.
         * 
         * <p>The user must have every single one of these permissions to access the endpoint.</p>
         * 
         * @param permissions the required permissions
         * @return this builder for method chaining
         */
        public EndpointProtectionBuilder requireAllPermissions(String... permissions) {
            this.permissions = Set.of(permissions);
            this.requireAllPermissions = true;
            return this;
        }
        
        /**
         * Explicitly requires authentication for this endpoint.
         * 
         * <p>This is the default behavior, so you don't usually need to call this method.</p>
         * 
         * @return this builder for method chaining
         */
        public EndpointProtectionBuilder requireAuthentication() {
            this.requiresAuthentication = true;
            this.allowAnonymous = false;
            return this;
        }
        
        /**
         * Allows unauthenticated (anonymous) access to this endpoint.
         * 
         * <p>Use this for public endpoints that don't require authentication.</p>
         * 
         * @return this builder for method chaining
         */
        public EndpointProtectionBuilder allowAnonymous() {
            this.allowAnonymous = true;
            this.requiresAuthentication = false;
            return this;
        }
        
        /**
         * Registers this security configuration with the {@link EndpointSecurityRegistry}.
         * 
         * <p>Call this method after you've finished configuring the security rule.
         * The configuration will be registered and will override any {@code @Secure}
         * annotation on the controller method.</p>
         */
        public void register() {
            EndpointSecurity security = EndpointSecurity.builder()
                    .roles(roles)
                    .permissions(permissions)
                    .requireAllRoles(requireAllRoles)
                    .requireAllPermissions(requireAllPermissions)
                    .allowAnonymous(allowAnonymous)
                    .requiresAuthentication(requiresAuthentication)
                    .build();
            
            registry.registerEndpoint(endpointPath, httpMethod, security);
            
            log.debug("Registered security for {} {} - Roles: {}, Permissions: {}, Auth required: {}", 
                    httpMethod, endpointPath, roles, permissions, requiresAuthentication);
        }
    }
}
