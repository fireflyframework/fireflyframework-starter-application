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

import java.util.UUID;

/**
 * <h1>Abstract Base Controller for Resource Endpoints</h1>
 * 
 * <p>This base class is for controllers that operate on <strong>contract and product resources</strong>.
 * It automatically resolves the full application context including party, tenant, contract, and product.
 * Both contractId and productId are REQUIRED - this controller enforces the complete resource hierarchy.</p>
 * 
 * <h2>When to Use</h2>
 * <p>Extend this class when building REST endpoints that operate on resources within a contract+product scope:</p>
 * <ul>
 *   <li><strong>Accounts:</strong> {@code /contracts/{contractId}/products/{productId}/accounts}</li>
 *   <li><strong>Transactions:</strong> {@code /contracts/{contractId}/products/{productId}/transactions}</li>
 *   <li><strong>Balances:</strong> {@code /contracts/{contractId}/products/{productId}/balances}</li>
 *   <li><strong>Cards:</strong> {@code /contracts/{contractId}/products/{productId}/cards}</li>
 *   <li><strong>Limits:</strong> {@code /contracts/{contractId}/products/{productId}/limits}</li>
 *   <li><strong>Beneficiaries:</strong> {@code /contracts/{contractId}/products/{productId}/beneficiaries}</li>
 * </ul>
 * 
 * <h2>Architecture</h2>
 * <p>This controller automatically resolves:</p>
 * <ul>
 *   <li><strong>Party ID:</strong> From Istio header <code>X-Party-Id</code></li>
 *   <li><strong>Tenant ID:</strong> Dynamically fetched from config-mgmt using party ID</li>
 *   <li><strong>Contract ID:</strong> From {@code @PathVariable UUID contractId} (REQUIRED)</li>
 *   <li><strong>Product ID:</strong> From {@code @PathVariable UUID productId} (REQUIRED)</li>
 *   <li><strong>Roles/Permissions:</strong> Enriched from FireflySessionManager based on party+contract+product</li>
 *   <li><strong>Tenant Config:</strong> Loaded from configuration service</li>
 * </ul>
 * 
 * <h2>Quick Example</h2>
 * <pre>
 * {@code
 * @RestController
 * @RequestMapping("/api/v1/contracts/{contractId}/products/{productId}/transactions")
 * public class TransactionController extends AbstractResourceController {
 *     
 *     @Autowired
 *     private TransactionApplicationService transactionService;
 *     
 *     @GetMapping
 *     @Secure(requireParty = true, requireContract = true, requireProduct = true, requireRole = "transaction:viewer")
 *     public Mono<List<TransactionDto>> listTransactions(
 *             @PathVariable UUID contractId,
 *             @PathVariable UUID productId,
 *             ServerWebExchange exchange) {
 *         
 *         // Automatically resolved context with party + tenant + contract + product
 *         return resolveExecutionContext(exchange, contractId, productId)
 *             .flatMap(context -> transactionService.listTransactions(context));
 *     }
 * }
 * }
 * </pre>
 * 
 * <h2>What You Get</h2>
 * <ul>
 *   <li><strong>Automatic Context Resolution:</strong> {@link #resolveExecutionContext(ServerWebExchange, UUID, UUID)}</li>
 *   <li><strong>Complete Resource Hierarchy:</strong> Party + Tenant + Contract + Product (all required)</li>
 *   <li><strong>Validation:</strong> {@link #requireContext(UUID, UUID)} ensures both IDs are not null</li>
 *   <li><strong>Full Config Access:</strong> Tenant configuration, feature flags, providers</li>
 *   <li><strong>Security Ready:</strong> Works seamlessly with {@code @Secure} annotations</li>
 * </ul>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see AbstractApplicationController For application-layer endpoints (no contract/product)
 */
@Slf4j
public abstract class AbstractResourceController {
    
    @Autowired
    private ContextResolver contextResolver;
    
    @Autowired
    private ConfigResolver configResolver;
    
    /**
     * Resolves the full application execution context for resource endpoints.
     * 
     * <p>This method:</p>
     * <ol>
     *   <li>Validates contractId and productId are not null (BOTH REQUIRED)</li>
     *   <li>Extracts party ID from Istio-injected <code>X-Party-Id</code> header</li>
     *   <li>Fetches tenant ID from config-mgmt using party ID</li>
     *   <li>Uses the provided contractId and productId from {@code @PathVariable}</li>
     *   <li>Enriches with roles and permissions from FireflySessionManager (party+contract+product scope)</li>
     *   <li>Loads tenant configuration</li>
     *   <li>Returns complete {@link ApplicationExecutionContext}</li>
     * </ol>
     * 
     * @param exchange the server web exchange
     * @param contractId the contract ID from {@code @PathVariable} (REQUIRED)
     * @param productId the product ID from {@code @PathVariable} (REQUIRED)
     * @return Mono of ApplicationExecutionContext with complete resource hierarchy
     * @throws IllegalArgumentException if contractId or productId is null
     */
    protected Mono<ApplicationExecutionContext> resolveExecutionContext(
            ServerWebExchange exchange, UUID contractId, UUID productId) {
        
        requireContext(contractId, productId);
        log.debug("Resolving resource execution context for contract: {}, product: {}", 
                contractId, productId);
        
        // Pass both contractId and productId (both required)
        return contextResolver.resolveContext(exchange, contractId, productId)
                .flatMap(appContext -> {
                    log.debug("Resolved resource context: party={}, tenant={}, contract={}, product={}", 
                            appContext.getPartyId(), appContext.getTenantId(), 
                            appContext.getContractId(), appContext.getProductId());
                    
                    return configResolver.resolveConfig(appContext.getTenantId())
                            .map(appConfig -> ApplicationExecutionContext.builder()
                                    .context(appContext)
                                    .config(appConfig)
                                    .build());
                })
                .doOnSuccess(ctx -> log.debug("Successfully resolved resource execution context"))
                .doOnError(error -> log.error("Failed to resolve resource execution context", error));
    }
    
    /**
     * Validates that both contractId and productId are not null.
     * 
     * <p><strong>IMPORTANT:</strong> This controller REQUIRES both contractId and productId.
     * Call this method (or let {@link #resolveExecutionContext} call it automatically) to ensure
     * both path variables are present.</p>
     * 
     * <p><strong>Example:</strong></p>
     * <pre>
     * {@code
     * @GetMapping("/{transactionId}")
     * public Mono<TransactionDto> getTransaction(
     *         @PathVariable UUID contractId,
     *         @PathVariable UUID productId,
     *         @PathVariable UUID transactionId) {
     *     requireContext(contractId, productId);  // Validates both IDs are present
     *     // ... rest of your logic
     * }
     * }
     * </pre>
     * 
     * @param contractId the contract ID from the path variable (REQUIRED)
     * @param productId the product ID from the path variable (REQUIRED)
     * @throws IllegalArgumentException if contractId or productId is null
     */
    protected final void requireContext(UUID contractId, UUID productId) {
        if (contractId == null) {
            log.error("Missing required path variable: contractId");
            throw new IllegalArgumentException(
                "contractId is required but was null. Check your @PathVariable mapping."
            );
        }
        if (productId == null) {
            log.error("Missing required path variable: productId");
            throw new IllegalArgumentException(
                "productId is required but was null. Check your @PathVariable mapping."
            );
        }
        log.trace("Resource context validated - Contract: {}, Product: {}", contractId, productId);
    }
    
    
    /**
     * Logs the current operation with full resource context.
     * 
     * <p>This is a convenience method for adding consistent, structured logging
     * to your endpoints. It logs at INFO level with both contract and product IDs.</p>
     * 
     * <p><strong>Example:</strong></p>
     * <pre>
     * {@code
     * @PostMapping
     * public Mono<TransactionDto> createTransaction(
     *         @PathVariable UUID contractId,
     *         @PathVariable UUID productId,
     *         @RequestBody CreateTransactionRequest request) {
     *     logOperation(contractId, productId, "createTransaction");
     *     return resolveExecutionContext(exchange, contractId, productId)
     *         .flatMap(context -> transactionService.createTransaction(context, request));
     * }
     * }
     * </pre>
     * 
     * @param contractId the contract ID
     * @param productId the product ID
     * @param operation a short description of the operation (e.g., "createTransaction", "listAccounts")
     */
    protected final void logOperation(UUID contractId, UUID productId, String operation) {
        log.info("[Resource] Contract: {}, Product: {}, Operation: {}", contractId, productId, operation);
    }
}
