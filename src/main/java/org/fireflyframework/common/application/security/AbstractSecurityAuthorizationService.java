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
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Abstract implementation of SecurityAuthorizationService.
 * Provides integration with Firefly SecurityCenter for authorization decisions.
 * 
 * <p>This class handles the core authorization logic and delegates to SecurityCenter
 * when needed. Subclasses can override specific methods for custom authorization logic.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractSecurityAuthorizationService implements SecurityAuthorizationService {
    
    @Override
    public Mono<AppSecurityContext> authorize(AppContext context, AppSecurityContext securityContext) {
        log.debug("Authorizing request for endpoint: {} {} by party: {}", 
                securityContext.getHttpMethod(), securityContext.getEndpoint(), context.getPartyId());
        
        // If anonymous access is allowed, grant immediately
        if (securityContext.isAllowAnonymous()) {
            return Mono.just(securityContext.withAuthorized(true)
                    .withConfigSource(AppSecurityContext.SecurityConfigSource.DEFAULT));
        }
        
        // Check if roles are required
        if (securityContext.hasRequiredRoles()) {
            return checkRoles(context, securityContext)
                    .flatMap(rolesOk -> {
                        if (!rolesOk) {
                            return Mono.just(createUnauthorizedContext(securityContext, "Required roles not present"));
                        }
                        // If permissions are also required, check them
                        if (securityContext.hasRequiredPermissions()) {
                            return checkPermissions(context, securityContext);
                        }
                        return Mono.just(createAuthorizedContext(securityContext));
                    });
        }
        
        // Check permissions if specified
        if (securityContext.hasRequiredPermissions()) {
            return checkPermissions(context, securityContext);
        }
        
        // If SecurityCenter should be used, delegate to it
        if (securityContext.getConfigSource() == AppSecurityContext.SecurityConfigSource.SECURITY_CENTER) {
            return authorizeWithSecurityCenter(context, securityContext);
        }
        
        // Default: allow access if no specific requirements
        log.debug("No specific security requirements, allowing access");
        return Mono.just(createAuthorizedContext(securityContext));
    }
    
    @Override
    public Mono<Boolean> hasRole(AppContext context, String role) {
        return Mono.just(context.hasRole(role));
    }
    
    @Override
    public Mono<Boolean> hasPermission(AppContext context, String permission) {
        return Mono.just(context.hasPermission(permission));
    }
    
    @Override
    public Mono<Boolean> evaluateExpression(AppContext context, String expression) {
        // TODO: Implement SpEL expression evaluation
        // This would use Spring's SpelExpressionParser to evaluate custom expressions
        log.warn("Expression evaluation not implemented: {}", expression);
        return Mono.just(false);
    }
    
    /**
     * Checks if the context has the required roles.
     * 
     * @param context the application context
     * @param securityContext the security context
     * @return Mono of boolean indicating if roles are satisfied
     */
    protected Mono<Boolean> checkRoles(AppContext context, AppSecurityContext securityContext) {
        if (securityContext.getRequiredRoles() == null || securityContext.getRequiredRoles().isEmpty()) {
            return Mono.just(true);
        }
        
        boolean hasRequiredRoles = securityContext.getRequiredRoles().stream()
                .anyMatch(context::hasRole);
        
        return Mono.just(hasRequiredRoles);
    }
    
    /**
     * Checks if the context has the required permissions.
     * 
     * @param context the application context
     * @param securityContext the security context
     * @return Mono of AppSecurityContext with authorization result
     */
    protected Mono<AppSecurityContext> checkPermissions(AppContext context, AppSecurityContext securityContext) {
        if (securityContext.getRequiredPermissions() == null || securityContext.getRequiredPermissions().isEmpty()) {
            return Mono.just(createAuthorizedContext(securityContext));
        }
        
        boolean hasRequiredPermissions = securityContext.getRequiredPermissions().stream()
                .anyMatch(context::hasPermission);
        
        if (hasRequiredPermissions) {
            return Mono.just(createAuthorizedContext(securityContext));
        } else {
            return Mono.just(createUnauthorizedContext(securityContext, "Required permissions not granted"));
        }
    }
    
    /**
     * Authorizes a request using the Firefly SecurityCenter.
     * 
     * <p>TODO: Implementation should integrate with SecurityCenter to evaluate
     * authorization policies based on the party, contract, product, and endpoint.</p>
     * 
     * @param context the application context
     * @param securityContext the security context
     * @return Mono of AppSecurityContext with authorization result
     */
    protected Mono<AppSecurityContext> authorizeWithSecurityCenter(AppContext context, 
                                                                   AppSecurityContext securityContext) {
        // TODO: Implement SecurityCenter integration
        // Example:
        // return securityCenterClient.authorize(
        //     AuthorizationRequest.builder()
        //         .partyId(context.getPartyId())
        //         .contractId(context.getContractId())
        //         .productId(context.getProductId())
        //         .endpoint(securityContext.getEndpoint())
        //         .httpMethod(securityContext.getHttpMethod())
        //         .roles(context.getRoles())
        //         .permissions(context.getPermissions())
        //         .build()
        // ).map(response -> {
        //     AppSecurityContext.SecurityEvaluationResult evaluationResult = 
        //         AppSecurityContext.SecurityEvaluationResult.builder()
        //             .granted(response.isGranted())
        //             .reason(response.getReason())
        //             .evaluatedPolicy(response.getPolicyName())
        //             .evaluationDetails(response.getDetails())
        //             .evaluatedAt(Instant.now())
        //             .build();
        //     
        //     return securityContext.toBuilder()
        //         .authorized(response.isGranted())
        //         .authorizationFailureReason(response.isGranted() ? null : response.getReason())
        //         .configSource(AppSecurityContext.SecurityConfigSource.SECURITY_CENTER)
        //         .evaluationResult(evaluationResult)
        //         .build();
        // });
        
        log.warn("SecurityCenter integration not implemented, denying access by default");
        return Mono.just(createUnauthorizedContext(securityContext, "SecurityCenter integration not implemented"));
    }
    
    /**
     * Creates an authorized security context.
     * 
     * @param original the original security context
     * @return authorized security context
     */
    protected AppSecurityContext createAuthorizedContext(AppSecurityContext original) {
        return original.withAuthorized(true)
                .withAuthorizationFailureReason(null);
    }
    
    /**
     * Creates an unauthorized security context with a reason.
     * 
     * @param original the original security context
     * @param reason the reason for denial
     * @return unauthorized security context
     */
    protected AppSecurityContext createUnauthorizedContext(AppSecurityContext original, String reason) {
        return original.withAuthorized(false)
                .withAuthorizationFailureReason(reason);
    }
}
