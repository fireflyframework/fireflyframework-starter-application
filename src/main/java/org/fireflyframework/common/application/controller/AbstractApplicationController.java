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

package org.fireflyframework.application.controller;

import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.resolver.ContextResolver;
import org.fireflyframework.application.resolver.ConfigResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <h1>Abstract Base Controller for Application-Layer Endpoints</h1>
 * 
 * <p>This base class is for controllers that operate on <strong>application-level resources</strong>
 * without requiring a contract or product context. Perfect for onboarding, product catalogs,
 * or any operation that only needs the authenticated party identity.</p>
 * 
 * <h2>When to Use</h2>
 * <p>Extend this class when building REST endpoints for:</p>
 * <ul>
 *   <li><strong>Onboarding:</strong> Customer registration, KYC verification</li>
 *   <li><strong>Product Catalog:</strong> Listing available products for a party</li>
 *   <li><strong>Party Profile:</strong> Managing party information, preferences</li>
 *   <li><strong>Contract Creation:</strong> Requesting new contracts or products</li>
 * </ul>
 * 
 * <h2>Architecture</h2>
 * <p>This controller automatically resolves:</p>
 * <ul>
 *   <li><strong>Party ID:</strong> Extracted from Istio-injected <code>X-Party-Id</code> header</li>
 *   <li><strong>Tenant ID:</strong> Extracted from Istio-injected <code>X-Tenant-Id</code> header</li>
 *   <li><strong>Roles/Permissions:</strong> Enriched from platform SDKs</li>
 *   <li><strong>Tenant Config:</strong> Loaded from configuration service</li>
 * </ul>
 * 
 * <h2>Quick Example</h2>
 * <pre>
 * {@code
 * @RestController
 * @RequestMapping("/api/v1/onboarding")
 * public class OnboardingController extends AbstractApplicationController {
 *     
 *     @Autowired
 *     private OnboardingApplicationService onboardingService;
 *     
 *     @PostMapping("/start")
 *     @Secure(requireParty = true)
 *     public Mono<OnboardingResponse> startOnboarding(
 *             @RequestBody OnboardingRequest request,
 *             ServerWebExchange exchange) {
 *         
 *         // Automatically resolved context with party + tenant
 *         return resolveExecutionContext(exchange)
 *             .flatMap(context -> onboardingService.startOnboarding(context, request));
 *     }
 * }
 * }
 * </pre>
 * 
 * <h2>What You Get</h2>
 * <ul>
 *   <li><strong>Automatic Context Resolution:</strong> {@link #resolveExecutionContext(ServerWebExchange)}</li>
 *   <li><strong>Party + Tenant Only:</strong> No contract or product IDs required</li>
 *   <li><strong>Full Config Access:</strong> Tenant configuration, feature flags, providers</li>
 *   <li><strong>Security Ready:</strong> Works seamlessly with {@code @Secure} annotations</li>
 * </ul>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see AbstractResourceController For resource endpoints (contract + product required)
 */
@Slf4j
public abstract class AbstractApplicationController {
    
    @Autowired
    private ContextResolver contextResolver;
    
    @Autowired
    private ConfigResolver configResolver;
    
    /**
     * Resolves the full application execution context for application-layer endpoints.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Extracts party ID and tenant ID from Istio headers</li>
     *   <li>Enriches with roles and permissions from platform SDKs</li>
     *   <li>Loads tenant configuration</li>
     *   <li>Returns complete {@link ApplicationExecutionContext}</li>
     * </ol>
     * 
     * <p><strong>Note:</strong> Contract and product IDs will be <code>null</code>
     * since this is an application-layer endpoint.</p>
     * 
     * @param exchange the server web exchange
     * @return Mono of ApplicationExecutionContext with party and tenant context
     */
    protected Mono<ApplicationExecutionContext> resolveExecutionContext(ServerWebExchange exchange) {
        log.debug("Resolving application-layer execution context (no contract/product)");
        
        // Pass null for contract and product since this is application-layer only
        return contextResolver.resolveContext(exchange, null, null)
                .flatMap(appContext -> {
                    log.debug("Resolved application context: party={}, tenant={}", 
                            appContext.getPartyId(), appContext.getTenantId());
                    
                    return configResolver.resolveConfig(appContext.getTenantId())
                            .map(appConfig -> ApplicationExecutionContext.builder()
                                    .context(appContext)
                                    .config(appConfig)
                                    .build());
                })
                .doOnSuccess(ctx -> log.debug("Successfully resolved application-layer execution context"))
                .doOnError(error -> log.error("Failed to resolve application-layer execution context", error));
    }
    
    /**
     * Logs the current operation with party context.
     * 
     * <p>Convenience method for consistent, structured logging.</p>
     * 
     * @param exchange the server web exchange
     * @param operation a short description of the operation (e.g., "startOnboarding", "submitKYC")
     */
    protected void logOperation(ServerWebExchange exchange, String operation) {
        log.info("[Party Operation] {}", operation);
    }
}
