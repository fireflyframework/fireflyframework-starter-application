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

package org.fireflyframework.application.resolver;

import org.fireflyframework.application.context.AppContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Interface for resolving application context from incoming requests.
 * Implementations are responsible for extracting and enriching context information
 * such as partyId, contractId, productId, roles, and permissions.
 * 
 * <p>This is the main entry point for context resolution in the application layer.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
public interface ContextResolver {
    
    /**
     * Resolves the complete application context from the request.
     * This method extracts all IDs automatically (party, tenant, contract, product).
     * 
     * @param exchange the server web exchange
     * @return Mono of resolved AppContext
     */
    Mono<AppContext> resolveContext(ServerWebExchange exchange);
    
    /**
     * Resolves the application context with explicit contractId and productId.
     * This is the method controllers should use to pass IDs extracted from {@code @PathVariable}.
     * 
     * <p>Party and tenant IDs are still extracted from Istio headers (X-Party-Id, X-Tenant-Id),
     * but contract and product IDs are provided explicitly by the controller.</p>
     * 
     * @param exchange the server web exchange
     * @param contractId the contract ID from {@code @PathVariable} (nullable)
     * @param productId the product ID from {@code @PathVariable} (nullable)
     * @return Mono of resolved AppContext
     */
    Mono<AppContext> resolveContext(ServerWebExchange exchange, UUID contractId, UUID productId);
    
    /**
     * Resolves the party ID from the request.
     * This should extract the authenticated user/customer identifier.
     * 
     * @param exchange the server web exchange
     * @return Mono of party UUID
     */
    Mono<UUID> resolvePartyId(ServerWebExchange exchange);
    
    /**
     * Resolves the contract ID from the request.
     * This may come from path parameters, query parameters, or headers.
     * 
     * @param exchange the server web exchange
     * @return Mono of contract UUID (may be empty)
     */
    Mono<UUID> resolveContractId(ServerWebExchange exchange);
    
    /**
     * Resolves the product ID from the request.
     * This may come from path parameters, query parameters, or headers.
     * 
     * @param exchange the server web exchange
     * @return Mono of product UUID (may be empty)
     */
    Mono<UUID> resolveProductId(ServerWebExchange exchange);
    
    /**
     * Resolves the tenant ID from the request.
     * This typically comes from authentication tokens or subdomain.
     * 
     * @param exchange the server web exchange
     * @return Mono of tenant UUID
     */
    Mono<UUID> resolveTenantId(ServerWebExchange exchange);
    
    /**
     * Checks if this resolver supports the given request.
     * Allows for multiple resolver implementations with different strategies.
     * 
     * @param exchange the server web exchange
     * @return true if this resolver can handle the request
     */
    default boolean supports(ServerWebExchange exchange) {
        return true;
    }
    
    /**
     * Priority of this resolver (higher values take precedence).
     * Used when multiple resolvers support the same request.
     * 
     * @return priority value
     */
    default int getPriority() {
        return 0;
    }
}
