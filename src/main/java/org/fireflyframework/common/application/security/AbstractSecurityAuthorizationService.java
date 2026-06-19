/*
 * Copyright 2024-2026 Firefly Software Foundation
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

package org.fireflyframework.common.application.security;

import org.fireflyframework.common.application.context.AppContext;
import org.fireflyframework.common.application.context.AppSecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import reactor.core.publisher.Mono;

/**
 * Abstract implementation of {@link SecurityAuthorizationService}.
 *
 * <p>Authorization decisions are derived solely from the roles and permissions already
 * resolved into the {@link AppContext}. This keeps the authorization layer fully
 * product-agnostic: the validated identity (subject, tenant, roles, permissions) is
 * resolved up-front by the context resolution layer, and this service simply evaluates
 * the declared requirements against it.</p>
 *
 * <p>Subclasses can override specific hook methods for custom authorization logic.</p>
 *
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractSecurityAuthorizationService implements SecurityAuthorizationService {

    private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    @Override
    public Mono<AppSecurityContext> authorize(AppContext context, AppSecurityContext securityContext) {
        log.debug("Authorizing request for endpoint: {} {} by subject: {}",
                securityContext.getHttpMethod(), securityContext.getEndpoint(), context.getSubject());

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

        // If a policy source is configured, delegate to it
        if (securityContext.getConfigSource() == AppSecurityContext.SecurityConfigSource.POLICY) {
            return authorizeWithPolicy(context, securityContext);
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
        if (expression == null || expression.isBlank()) {
            return Mono.just(true);
        }
        try {
            EvaluationContext evalContext = new StandardEvaluationContext();
            ((StandardEvaluationContext) evalContext).setVariable("context", context);
            ((StandardEvaluationContext) evalContext).setRootObject(context);
            Expression parsed = EXPRESSION_PARSER.parseExpression(expression);
            Boolean result = parsed.getValue(evalContext, Boolean.class);
            log.debug("SpEL expression '{}' evaluated to: {}", expression, result);
            return Mono.just(result != null && result);
        } catch (Exception e) {
            log.error("Failed to evaluate SpEL expression '{}': {}", expression, e.getMessage());
            return Mono.just(false);
        }
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
     * Authorizes a request using an external policy source.
     *
     * <p>The default implementation evaluates the declared role and permission requirements
     * against the roles and permissions already present in the {@link AppContext}. Subclasses
     * may override this method to integrate with a dedicated policy decision point (PDP) for
     * attribute-based or policy-based access control.</p>
     *
     * @param context the application context
     * @param securityContext the security context
     * @return Mono of AppSecurityContext with authorization result
     */
    protected Mono<AppSecurityContext> authorizeWithPolicy(AppContext context,
                                                           AppSecurityContext securityContext) {
        // Default policy evaluation falls back to role/permission checks against the resolved context.
        if (securityContext.hasRequiredRoles()) {
            return checkRoles(context, securityContext)
                    .flatMap(rolesOk -> {
                        if (!rolesOk) {
                            return Mono.just(createUnauthorizedContext(securityContext, "Required roles not present"));
                        }
                        if (securityContext.hasRequiredPermissions()) {
                            return checkPermissions(context, securityContext);
                        }
                        return Mono.just(createAuthorizedContext(securityContext));
                    });
        }
        if (securityContext.hasRequiredPermissions()) {
            return checkPermissions(context, securityContext);
        }
        return Mono.just(createAuthorizedContext(securityContext));
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
