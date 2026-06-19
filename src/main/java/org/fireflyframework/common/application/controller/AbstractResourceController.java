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
 * <h1>Abstract Base Controller for Resource Endpoints</h1>
 *
 * <p>This base class is a thin, product-agnostic foundation for REST controllers. It resolves the
 * request {@link AppContext} (subject, tenant, roles, permissions) from the validated security
 * context via the {@link ContextResolver}, and loads the tenant {@link org.fireflyframework.common.application.context.AppConfig}
 * via the {@link ConfigResolver}, exposing both as a single {@link ApplicationExecutionContext}.</p>
 *
 * <h2>When to Use</h2>
 * <p>Extend this class when building REST endpoints that need the authenticated identity and tenant
 * configuration. There is no built-in resource hierarchy or scoping &mdash; model any
 * domain-specific path segments with your own {@code @PathVariable} parameters.</p>
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
 * @RequestMapping("/api/v1/transactions")
 * public class TransactionController extends AbstractResourceController {
 *
 *     @Autowired
 *     private TransactionApplicationService transactionService;
 *
 *     @GetMapping
 *     @Secure(requireRole = "transaction:viewer")
 *     public Mono<List<TransactionDto>> listTransactions(ServerWebExchange exchange) {
 *
 *         // Automatically resolved context with subject + tenant + roles + permissions
 *         return resolveExecutionContext(exchange)
 *             .flatMap(context -> transactionService.listTransactions(context));
 *     }
 * }
 * }
 * </pre>
 *
 * <h2>What You Get</h2>
 * <ul>
 *   <li><strong>Automatic Context Resolution:</strong> {@link #resolveExecutionContext(ServerWebExchange)}</li>
 *   <li><strong>Raw Context Access:</strong> {@link #resolveContext(ServerWebExchange)}</li>
 *   <li><strong>Full Config Access:</strong> Tenant configuration, feature flags, providers</li>
 *   <li><strong>Security Ready:</strong> Works seamlessly with {@code @Secure} annotations</li>
 * </ul>
 *
 * @author Firefly Development Team
 * @since 1.0.0
 * @see AbstractApplicationController For application-layer endpoints
 */
@Slf4j
public abstract class AbstractResourceController {

    @Autowired
    private ContextResolver contextResolver;

    @Autowired
    private ConfigResolver configResolver;

    /**
     * Resolves the raw {@link AppContext} for the request.
     *
     * <p>Delegates to the configured {@link ContextResolver}, which derives the subject, tenant,
     * roles and permissions from the validated security context.</p>
     *
     * @param exchange the server web exchange
     * @return Mono of AppContext (subject, tenant, roles, permissions, attributes)
     */
    protected Mono<AppContext> resolveContext(ServerWebExchange exchange) {
        return contextResolver.resolveContext(exchange);
    }

    /**
     * Resolves the full {@link ApplicationExecutionContext} for the request.
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
     * @return Mono of ApplicationExecutionContext with context and config
     */
    protected Mono<ApplicationExecutionContext> resolveExecutionContext(ServerWebExchange exchange) {
        log.debug("Resolving execution context");

        return contextResolver.resolveContext(exchange)
                .flatMap(appContext -> {
                    log.debug("Resolved context: subject={}, tenant={}",
                            appContext.getSubject(), appContext.getTenantId());

                    return configResolver.resolveConfig(appContext.getTenantId())
                            .map(appConfig -> ApplicationExecutionContext.builder()
                                    .context(appContext)
                                    .config(appConfig)
                                    .build());
                })
                .doOnSuccess(ctx -> log.debug("Successfully resolved execution context"))
                .doOnError(error -> log.error("Failed to resolve execution context", error));
    }

    /**
     * Logs the current operation.
     *
     * <p>This is a convenience method for adding consistent, structured logging to your endpoints.
     * It logs at INFO level.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * {@code
     * @PostMapping
     * public Mono<TransactionDto> createTransaction(
     *         @RequestBody CreateTransactionRequest request,
     *         ServerWebExchange exchange) {
     *     logOperation("createTransaction");
     *     return resolveExecutionContext(exchange)
     *         .flatMap(context -> transactionService.createTransaction(context, request));
     * }
     * }
     * </pre>
     *
     * @param operation a short description of the operation (e.g., "createTransaction", "listAccounts")
     */
    protected final void logOperation(String operation) {
        log.info("[Operation] {}", operation);
    }
}
