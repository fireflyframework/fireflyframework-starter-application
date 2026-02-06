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

import org.fireflyframework.application.context.AppSecurityContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for explicit endpoint-to-security mappings.
 * Provides an alternative to annotation-based security configuration.
 * 
 * <p>This registry allows programmatic registration of security requirements
 * for endpoints, which is useful for dynamic security configuration or when
 * annotations cannot be used.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * registry.registerEndpoint(
 *     "/api/v1/accounts/{id}/transfer",
 *     "POST",
 *     EndpointSecurity.builder()
 *         .roles(Set.of("ACCOUNT_OWNER"))
 *         .permissions(Set.of("TRANSFER_FUNDS"))
 *         .build()
 * );
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
public class EndpointSecurityRegistry {
    
    private final Map<String, EndpointSecurity> endpointMap = new ConcurrentHashMap<>();
    
    /**
     * Registers security requirements for an endpoint.
     * 
     * @param endpoint the endpoint path (with path variables)
     * @param httpMethod the HTTP method
     * @param security the security configuration
     */
    public void registerEndpoint(String endpoint, String httpMethod, EndpointSecurity security) {
        String key = createKey(endpoint, httpMethod);
        endpointMap.put(key, security);
        log.debug("Registered security for endpoint: {} {}", httpMethod, endpoint);
    }
    
    /**
     * Gets security configuration for an endpoint.
     * 
     * @param endpoint the endpoint path
     * @param httpMethod the HTTP method
     * @return Optional containing security config if registered
     */
    public Optional<EndpointSecurity> getEndpointSecurity(String endpoint, String httpMethod) {
        String key = createKey(endpoint, httpMethod);
        return Optional.ofNullable(endpointMap.get(key));
    }
    
    /**
     * Checks if an endpoint is registered.
     * 
     * @param endpoint the endpoint path
     * @param httpMethod the HTTP method
     * @return true if the endpoint is registered
     */
    public boolean isRegistered(String endpoint, String httpMethod) {
        String key = createKey(endpoint, httpMethod);
        return endpointMap.containsKey(key);
    }
    
    /**
     * Unregisters an endpoint.
     * 
     * @param endpoint the endpoint path
     * @param httpMethod the HTTP method
     */
    public void unregisterEndpoint(String endpoint, String httpMethod) {
        String key = createKey(endpoint, httpMethod);
        endpointMap.remove(key);
        log.debug("Unregistered security for endpoint: {} {}", httpMethod, endpoint);
    }
    
    /**
     * Clears all registered endpoints.
     */
    public void clear() {
        endpointMap.clear();
        log.info("Cleared all endpoint security registrations");
    }
    
    /**
     * Gets all registered endpoints.
     * 
     * @return map of endpoint keys to security configurations
     */
    public Map<String, EndpointSecurity> getAllEndpoints() {
        return Map.copyOf(endpointMap);
    }
    
    private String createKey(String endpoint, String httpMethod) {
        return httpMethod.toUpperCase() + ":" + endpoint;
    }
    
    /**
     * Endpoint security configuration.
     */
    public static class EndpointSecurity {
        private final Set<String> roles;
        private final Set<String> permissions;
        private final boolean requireAllRoles;
        private final boolean requireAllPermissions;
        private final boolean allowAnonymous;
        private final boolean requiresAuthentication;
        private final String expression;
        private final Map<String, String> attributes;
        
        private EndpointSecurity(Builder builder) {
            this.roles = builder.roles;
            this.permissions = builder.permissions;
            this.requireAllRoles = builder.requireAllRoles;
            this.requireAllPermissions = builder.requireAllPermissions;
            this.allowAnonymous = builder.allowAnonymous;
            this.requiresAuthentication = builder.requiresAuthentication;
            this.expression = builder.expression;
            this.attributes = builder.attributes;
        }
        
        public Set<String> getRoles() {
            return roles;
        }
        
        public Set<String> getPermissions() {
            return permissions;
        }
        
        public boolean isRequireAllRoles() {
            return requireAllRoles;
        }
        
        public boolean isRequireAllPermissions() {
            return requireAllPermissions;
        }
        
        public boolean isAllowAnonymous() {
            return allowAnonymous;
        }
        
        public boolean isRequiresAuthentication() {
            return requiresAuthentication;
        }
        
        public String getExpression() {
            return expression;
        }
        
        public Map<String, String> getAttributes() {
            return attributes;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        /**
         * Converts to AppSecurityContext.
         * 
         * @param endpoint the endpoint
         * @param httpMethod the HTTP method
         * @return AppSecurityContext
         */
        public AppSecurityContext toSecurityContext(String endpoint, String httpMethod) {
            return AppSecurityContext.builder()
                    .endpoint(endpoint)
                    .httpMethod(httpMethod)
                    .requiredRoles(roles)
                    .requiredPermissions(permissions)
                    .requiresAuthentication(requiresAuthentication)
                    .allowAnonymous(allowAnonymous)
                    .configSource(AppSecurityContext.SecurityConfigSource.EXPLICIT_MAP)
                    .build();
        }
        
        public static class Builder {
            private Set<String> roles = Set.of();
            private Set<String> permissions = Set.of();
            private boolean requireAllRoles = false;
            private boolean requireAllPermissions = false;
            private boolean allowAnonymous = false;
            private boolean requiresAuthentication = true;
            private String expression = "";
            private Map<String, String> attributes = Map.of();
            
            public Builder roles(Set<String> roles) {
                this.roles = roles;
                return this;
            }
            
            public Builder permissions(Set<String> permissions) {
                this.permissions = permissions;
                return this;
            }
            
            public Builder requireAllRoles(boolean requireAllRoles) {
                this.requireAllRoles = requireAllRoles;
                return this;
            }
            
            public Builder requireAllPermissions(boolean requireAllPermissions) {
                this.requireAllPermissions = requireAllPermissions;
                return this;
            }
            
            public Builder allowAnonymous(boolean allowAnonymous) {
                this.allowAnonymous = allowAnonymous;
                return this;
            }
            
            public Builder requiresAuthentication(boolean requiresAuthentication) {
                this.requiresAuthentication = requiresAuthentication;
                return this;
            }
            
            public Builder expression(String expression) {
                this.expression = expression;
                return this;
            }
            
            public Builder attributes(Map<String, String> attributes) {
                this.attributes = attributes;
                return this;
            }
            
            public EndpointSecurity build() {
                return new EndpointSecurity(this);
            }
        }
    }
}
