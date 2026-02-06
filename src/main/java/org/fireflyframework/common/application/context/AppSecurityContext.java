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

package org.fireflyframework.application.context;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.Map;
import java.util.Set;

/**
 * Immutable security context for application requests.
 * Contains security-related information including endpoint-role mappings and authorization results.
 * 
 * <p>This class is used in conjunction with the Firefly SecurityCenter to determine
 * whether a party has sufficient rights to perform an operation based on their role
 * in a contract/product context.</p>
 * 
 * <p>Security context can be configured in two ways:
 * <ul>
 *   <li>Declarative: Using @Secure annotation on endpoints/controllers</li>
 *   <li>Programmatic: Explicit endpoint-role mapping registration</li>
 * </ul>
 * </p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
@With
public class AppSecurityContext {
    
    /**
     * The endpoint being accessed (e.g., "/api/v1/accounts/{id}/transfer")
     */
    String endpoint;
    
    /**
     * The HTTP method being used (GET, POST, PUT, DELETE, etc.)
     */
    String httpMethod;
    
    /**
     * Roles required to access this endpoint
     */
    Set<String> requiredRoles;
    
    /**
     * Permissions required to access this endpoint
     */
    Set<String> requiredPermissions;
    
    /**
     * Whether authorization was successful
     */
    boolean authorized;
    
    /**
     * Reason for authorization failure (if applicable)
     */
    String authorizationFailureReason;
    
    /**
     * Source of the security configuration (ANNOTATION, EXPLICIT_MAP, SECURITY_CENTER)
     */
    SecurityConfigSource configSource;
    
    /**
     * Additional security attributes
     */
    Map<String, Object> securityAttributes;
    
    /**
     * Whether this endpoint requires authentication
     */
    @Builder.Default
    boolean requiresAuthentication = true;
    
    /**
     * Whether this endpoint allows anonymous access
     */
    @Builder.Default
    boolean allowAnonymous = false;
    
    /**
     * Custom security evaluation result from SecurityCenter
     */
    SecurityEvaluationResult evaluationResult;
    
    /**
     * Checks if the security context requires any roles
     * 
     * @return true if roles are required
     */
    public boolean hasRequiredRoles() {
        return requiredRoles != null && !requiredRoles.isEmpty();
    }
    
    /**
     * Checks if the security context requires any permissions
     * 
     * @return true if permissions are required
     */
    public boolean hasRequiredPermissions() {
        return requiredPermissions != null && !requiredPermissions.isEmpty();
    }
    
    /**
     * Checks if a specific role is required
     * 
     * @param role the role to check
     * @return true if the role is required
     */
    public boolean requiresRole(String role) {
        return requiredRoles != null && requiredRoles.contains(role);
    }
    
    /**
     * Checks if a specific permission is required
     * 
     * @param permission the permission to check
     * @return true if the permission is required
     */
    public boolean requiresPermission(String permission) {
        return requiredPermissions != null && requiredPermissions.contains(permission);
    }
    
    /**
     * Gets a security attribute
     * 
     * @param key the attribute key
     * @param <T> the expected type
     * @return the attribute value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getSecurityAttribute(String key) {
        return securityAttributes != null ? (T) securityAttributes.get(key) : null;
    }
    
    /**
     * Source of security configuration
     */
    public enum SecurityConfigSource {
        /**
         * Security configuration from @Secure annotation
         */
        ANNOTATION,
        
        /**
         * Security configuration from explicit endpoint-role mapping
         */
        EXPLICIT_MAP,
        
        /**
         * Security configuration from Firefly SecurityCenter
         */
        SECURITY_CENTER,
        
        /**
         * Security configuration from default/fallback rules
         */
        DEFAULT
    }
    
    /**
     * Result of security evaluation from SecurityCenter
     */
    @Value
    @Builder(toBuilder = true)
    @With
    public static class SecurityEvaluationResult {
        
        /**
         * Whether access is granted
         */
        boolean granted;
        
        /**
         * Reason for the decision
         */
        String reason;
        
        /**
         * Rule or policy that was evaluated
         */
        String evaluatedPolicy;
        
        /**
         * Additional evaluation details
         */
        Map<String, Object> evaluationDetails;
        
        /**
         * Timestamp of evaluation
         */
        java.time.Instant evaluatedAt;
        
        /**
         * Gets an evaluation detail
         * 
         * @param key the detail key
         * @param <T> the expected type
         * @return the detail value or null if not found
         */
        @SuppressWarnings("unchecked")
        public <T> T getEvaluationDetail(String key) {
            return evaluationDetails != null ? (T) evaluationDetails.get(key) : null;
        }
    }
}
