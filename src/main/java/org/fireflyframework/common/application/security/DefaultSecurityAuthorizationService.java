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

import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.context.AppSecurityContext;
import org.fireflyframework.application.util.SessionContextMapper;
import org.fireflyframework.common.application.spi.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Default implementation of SecurityAuthorizationService.
 * 
 * <p><strong>This is provided by the library - microservices don't need to implement anything.</strong></p>
 * 
 * <p>This service automatically:</p>
 * <ul>
 *   <li>Performs role-based authorization</li>
 *   <li>Performs permission-based authorization</li>
 *   <li>Supports requireAll vs requireAny semantics</li>
 *   <li>Optionally integrates with SecurityCenter for complex policies</li>
 * </ul>
 * 
 * <h2>What Microservices Need to Do</h2>
 * <p><strong>Nothing.</strong> Authorization works automatically based on:</p>
 * <ul>
 *   <li><code>@Secure</code> annotations on controllers/methods</li>
 *   <li>Programmatic security rules in <code>EndpointSecurityRegistry</code></li>
 * </ul>
 * 
 * <h2>Authorization Logic</h2>
 * <p>By default, this service checks if:</p>
 * <ol>
 *   <li>User's roles (from context) match required roles</li>
 *   <li>User's permissions (from context) match required permissions</li>
 *   <li>If SecurityCenter is enabled, delegates complex policy evaluation</li>
 * </ol>
 * 
 * <h2>SecurityCenter Integration</h2>
 * <p>When SecurityCenter SDK is available, complex authorization policies
 * will be evaluated by SecurityCenter for:</p>
 * <ul>
 *   <li>Attribute-Based Access Control (ABAC)</li>
 *   <li>Policy-based decisions</li>
 *   <li>Audit trail</li>
 * </ul>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultSecurityAuthorizationService extends AbstractSecurityAuthorizationService {
    
    @Autowired(required = false)
    private final SessionManager sessionManager;
    
    // The parent AbstractSecurityAuthorizationService already provides:
    // - Role checking (hasRole, hasAnyRole, hasAllRoles)
    // - Permission checking (hasPermission, hasAnyPermission, hasAllPermissions)
    // - Authorization with requireAll/requireAny semantics
    
    /**
     * Enhanced authorization using SessionManager for product access validation.
     * 
     * <p>This method integrates with the Security Center's SessionManager to:</p>
     * <ul>
     *   <li>Validate party has access to specific products/contracts</li>
     *   <li>Check granular permissions (action + resource)</li>
     *   <li>Provide graceful degradation if SecurityCenter is unavailable</li>
     * </ul>
     */
    @Override
    protected Mono<AppSecurityContext> authorizeWithSecurityCenter(
            AppContext context, 
            AppSecurityContext securityContext) {
        
        if (sessionManager == null) {
            log.warn("SessionManager not available - falling back to basic role/permission checks. " +
                    "Deploy common-platform-security-center for enhanced authorization.");
            return super.authorizeWithSecurityCenter(context, securityContext);
        }
        
        log.debug("Using SessionManager for authorization: party={}, contract={}, product={}",
                context.getPartyId(), context.getContractId(), context.getProductId());
        
        // If product access needs to be validated
        if (context.getProductId() != null) {
            return sessionManager.hasAccessToProduct(context.getPartyId(), context.getProductId())
                .flatMap(hasAccess -> {
                    if (!hasAccess) {
                        log.warn("Party {} does not have access to product {}", 
                                context.getPartyId(), context.getProductId());
                        return Mono.just(createUnauthorizedContext(securityContext, 
                                "No access to requested product"));
                    }
                    
                    // Product access OK - now check role/permission requirements
                    return performRolePermissionChecks(context, securityContext);
                })
                .doOnError(error -> log.error("Error checking product access via SessionManager: {}", 
                        error.getMessage(), error))
                .onErrorResume(error -> {
                    // Graceful degradation on error
                    log.warn("Falling back to basic checks due to error: {}", error.getMessage());
                    return performRolePermissionChecks(context, securityContext);
                });
        }
        
        // No product context - just perform role/permission checks
        return performRolePermissionChecks(context, securityContext);
    }
    
    /**
     * Performs standard role and permission checks using the AppContext.
     */
    private Mono<AppSecurityContext> performRolePermissionChecks(
            AppContext context, AppSecurityContext securityContext) {
        
        // Check required roles
        if (securityContext.hasRequiredRoles()) {
            return checkRoles(context, securityContext)
                .flatMap(rolesOk -> {
                    if (!rolesOk) {
                        return Mono.just(createUnauthorizedContext(securityContext, 
                                "Required roles not present"));
                    }
                    // Roles OK - check permissions if needed
                    if (securityContext.hasRequiredPermissions()) {
                        return checkPermissions(context, securityContext);
                    }
                    return Mono.just(createAuthorizedContext(securityContext));
                });
        }
        
        // Check permissions
        if (securityContext.hasRequiredPermissions()) {
            return checkPermissions(context, securityContext);
        }
        
        // No specific requirements - grant access
        return Mono.just(createAuthorizedContext(securityContext));
    }
}
