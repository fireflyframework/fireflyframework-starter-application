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

package org.fireflyframework.common.application.controller;

import org.fireflyframework.common.application.context.ApplicationExecutionContext;
import org.fireflyframework.common.application.context.AppContext;
import org.fireflyframework.common.application.resolver.ContextResolver;
import org.fireflyframework.common.application.resolver.ConfigResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <h1>Abstract Base Controller for Application-Layer Endpoints</h1>
 *
 * <p>This base class is for controllers that operate on <strong>application-level resources</strong>.
 * It resolves the authenticated identity and tenant configuration into a single
 * {@link ApplicationExecutionContext}, perfect for onboarding, catalogs, or any operation that only
 * needs the authenticated subject and tenant.</p>
 *
 * <h2>When to Use</h2>
 * <p>Extend this class when building REST endpoints for:</p>
 * <ul>
 *   <li><strong>Onboarding:</strong> Registration and verification flows</li>
 *   <li><strong>Catalogs:</strong> Listing resources available to the subject</li>
 *   <li><strong>Profile:</strong> Managing subject information and preferences</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>This controller automatically resolves:</p>
 * <ul>
 *   <li><strong>Subject:</strong> The authenticated subject from the validated security context</li>
 *   <li><strong>Tenant ID:</strong> The tenant the subject belongs to</li>
 *   <li><strong>Roles/Permissions:</strong> Authorities and scopes resolved for the subject</li>
 *   <li><strong>Tenant Config:</strong> Loaded from the configuration service</li>
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
 *     @Secure
 *     public Mono<OnboardingResponse> startOnboarding(
 *             @RequestBody OnboardingRequest request,
 *             ServerWebExchange exchange) {
 *
 *         // Automatically resolved context with subject + tenant
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
 *   <li><strong>Subject + Tenant:</strong> The authenticated identity and its tenant</li>
 *   <li><strong>Full Config Access:</strong> Tenant configuration, feature flags, providers</li>
 *   <li><strong>Security Ready:</strong> Works seamlessly with {@code @Secure} annotations</li>
 * </ul>
 *
 * @author Firefly Development Team
 * @since 1.0.0
 * @see AbstractResourceController For a thin generic base controller
 */
@Slf4j
public abstract class AbstractApplicationController {

    @Autowired
    private ContextResolver contextResolver;

    @Autowired
    private ConfigResolver configResolver;

    /**
     * Resolves the full {@link ApplicationExecutionContext} for application-layer endpoints.
     *
     * <p>This method:</p>
     * <ol>
     *   <li>Resolves the {@link AppContext} (subject, tenant, roles, permissions) from the
     *       validated security context</li>
     *   <li>Loads the tenant configuration</li>
     *   <li>Returns the complete {@link ApplicationExecutionContext}</li>
     * </ol>
     *
     * @param exchange the server web exchange
     * @return Mono of ApplicationExecutionContext with subject and tenant context
     */
    protected Mono<ApplicationExecutionContext> resolveExecutionContext(ServerWebExchange exchange) {
        log.debug("Resolving application-layer execution context");

        return contextResolver.resolveContext(exchange)
                .flatMap(appContext -> {
                    log.debug("Resolved application context: subject={}, tenant={}",
                            appContext.getSubject(), appContext.getTenantId());

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
     * Logs the current operation.
     *
     * <p>Convenience method for consistent, structured logging.</p>
     *
     * @param exchange the server web exchange
     * @param operation a short description of the operation (e.g., "startOnboarding", "submitKYC")
     */
    protected void logOperation(ServerWebExchange exchange, String operation) {
        log.info("[Operation] {}", operation);
    }
}
