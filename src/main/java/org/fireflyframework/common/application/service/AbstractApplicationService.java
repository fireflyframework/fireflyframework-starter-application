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

package org.fireflyframework.application.service;

import org.fireflyframework.application.context.AppConfig;
import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.context.AppMetadata;
import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.resolver.ConfigResolver;
import org.fireflyframework.application.resolver.ContextResolver;
import org.fireflyframework.application.security.SecurityAuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Abstract base class for application layer services.
 * Provides common functionality for context resolution, security, and business process orchestration.
 * 
 * <p>Application layer services are responsible for:</p>
 * <ul>
 *   <li>Orchestrating business processes across multiple domain services</li>
 *   <li>Managing application context (party, contract, product)</li>
 *   <li>Enforcing security and authorization policies</li>
 *   <li>Coordinating with external platform services</li>
 * </ul>
 * 
 * <p>Typical usage:</p>
 * <pre>
 * public class AccountApplicationService extends AbstractApplicationService {
 *     
 *     public Mono&lt;Transfer&gt; transferFunds(ServerWebExchange exchange, TransferRequest request) {
 *         return resolveExecutionContext(exchange)
 *             .flatMap(context -> {
 *                 // Business logic here
 *                 return performTransfer(context, request);
 *             });
 *     }
 * }
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractApplicationService {
    
    protected final ContextResolver contextResolver;
    protected final ConfigResolver configResolver;
    protected final SecurityAuthorizationService authorizationService;
    
    /**
     * Constructor with required dependencies.
     * 
     * @param contextResolver the context resolver
     * @param configResolver the config resolver
     * @param authorizationService the authorization service
     */
    protected AbstractApplicationService(ContextResolver contextResolver,
                                        ConfigResolver configResolver,
                                        SecurityAuthorizationService authorizationService) {
        this.contextResolver = contextResolver;
        this.configResolver = configResolver;
        this.authorizationService = authorizationService;
    }
    
    /**
     * Resolves the complete application execution context from the request.
     * This includes metadata, business context, and configuration.
     * 
     * @param exchange the server web exchange
     * @return Mono of ApplicationExecutionContext
     */
    protected Mono<ApplicationExecutionContext> resolveExecutionContext(ServerWebExchange exchange) {
        log.debug("Resolving execution context for request");
        
        return contextResolver.resolveContext(exchange)
                .flatMap(appContext -> 
                    configResolver.resolveConfig(appContext.getTenantId())
                        .map(appConfig -> ApplicationExecutionContext.builder()
                                .context(appContext)
                                .config(appConfig)
                                .build())
                )
                .doOnSuccess(ctx -> log.debug("Successfully resolved execution context for party: {}", ctx.getPartyId()))
                .doOnError(error -> log.error("Failed to resolve execution context", error));
    }
    
    /**
     * Validates that the execution context has required components.
     * 
     * @param context the execution context
     * @param requireContract whether contract ID is required
     * @param requireProduct whether product ID is required
     * @return Mono of validated context
     */
    protected Mono<ApplicationExecutionContext> validateContext(ApplicationExecutionContext context,
                                                               boolean requireContract,
                                                               boolean requireProduct) {
        if (requireContract && !context.getContext().hasContract()) {
            return Mono.error(new IllegalStateException("Contract ID is required but not present in context"));
        }
        
        if (requireProduct && !context.getContext().hasProduct()) {
            return Mono.error(new IllegalStateException("Product ID is required but not present in context"));
        }
        
        return Mono.just(context);
    }
    
    /**
     * Checks if the party has the required role.
     * 
     * @param context the execution context
     * @param role the required role
     * @return Mono that completes if role is present, errors otherwise
     */
    protected Mono<Void> requireRole(ApplicationExecutionContext context, String role) {
        return authorizationService.hasRole(context.getContext(), role)
                .flatMap(hasRole -> {
                    if (!hasRole) {
                        return Mono.error(new org.springframework.security.access.AccessDeniedException(
                                "Required role not present: " + role));
                    }
                    return Mono.empty();
                });
    }
    
    /**
     * Checks if the party has the required permission.
     * 
     * @param context the execution context
     * @param permission the required permission
     * @return Mono that completes if permission is granted, errors otherwise
     */
    protected Mono<Void> requirePermission(ApplicationExecutionContext context, String permission) {
        return authorizationService.hasPermission(context.getContext(), permission)
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return Mono.error(new org.springframework.security.access.AccessDeniedException(
                                "Required permission not granted: " + permission));
                    }
                    return Mono.empty();
                });
    }
    
    /**
     * Gets a provider configuration for the tenant.
     * 
     * @param context the execution context
     * @param providerType the provider type
     * @return Mono of provider config
     */
    protected Mono<AppConfig.ProviderConfig> getProviderConfig(ApplicationExecutionContext context, 
                                                              String providerType) {
        return Mono.justOrEmpty(context.getConfig().getProvider(providerType))
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "Provider not configured: " + providerType)));
    }
    
    /**
     * Checks if a feature is enabled for the tenant.
     * 
     * @param context the execution context
     * @param feature the feature flag name
     * @return Mono of boolean
     */
    protected Mono<Boolean> isFeatureEnabled(ApplicationExecutionContext context, String feature) {
        return Mono.just(context.isFeatureEnabled(feature));
    }
}
