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
import org.fireflyframework.application.context.AppMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

/**
 * Abstract base implementation of ContextResolver.
 * Provides common functionality and template methods for context resolution.
 * 
 * <p>Subclasses should implement the abstract methods to provide specific
 * resolution strategies for their use case.</p>
 * 
 * <p>This class integrates with platform SDKs to fetch context data:</p>
 * <ul>
 *   <li>common-platform-customer-mgmt-sdk: For party/customer information</li>
 *   <li>common-platform-contract-mgmt-sdk: For contract information</li>
 *   <li>common-platform-product-mgmt: For product information</li>
 * </ul>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractContextResolver implements ContextResolver {
    
    @Override
    public Mono<AppContext> resolveContext(ServerWebExchange exchange) {
        log.debug("Resolving application context for request (deprecated - use version with explicit IDs)");
        
        return Mono.zip(
                resolvePartyId(exchange),
                resolveTenantId(exchange),
                resolveContractId(exchange).defaultIfEmpty(UUID.randomUUID()), // placeholder UUID if empty
                resolveProductId(exchange).defaultIfEmpty(UUID.randomUUID())  // placeholder UUID if empty
        )
        .flatMap(tuple -> {
            UUID partyId = tuple.getT1();
            UUID tenantId = tuple.getT2();
            UUID contractId = tuple.getT3();
            UUID productId = tuple.getT4();
            
            return enrichContext(
                    AppContext.builder()
                            .partyId(partyId)
                            .tenantId(tenantId)
                            .contractId(contractId)
                            .productId(productId)
                            .build(),
                    exchange
            );
        })
        .doOnSuccess(context -> log.debug("Successfully resolved context for party: {}", context.getPartyId()))
        .doOnError(error -> log.error("Failed to resolve context", error));
    }
    
    @Override
    public Mono<AppContext> resolveContext(ServerWebExchange exchange, UUID contractId, UUID productId) {
        log.debug("Resolving application context with explicit contract: {} and product: {}", contractId, productId);
        
        return Mono.zip(
                resolvePartyId(exchange),
                resolveTenantId(exchange)
        )
        .flatMap(tuple -> {
            UUID partyId = tuple.getT1();
            UUID tenantId = tuple.getT2();
            
            return enrichContext(
                    AppContext.builder()
                            .partyId(partyId)
                            .tenantId(tenantId)
                            .contractId(contractId)  // Explicit from controller
                            .productId(productId)     // Explicit from controller
                            .build(),
                    exchange
            );
        })
        .doOnSuccess(context -> log.debug("Successfully resolved context for party: {}, contract: {}, product: {}", 
                context.getPartyId(), context.getContractId(), context.getProductId()))
        .doOnError(error -> log.error("Failed to resolve context", error));
    }
    
    /**
     * Enriches the basic context with roles, permissions, and additional data.
     * This method should fetch data from platform services.
     * 
     * @param basicContext the basic context with IDs
     * @param exchange the server web exchange
     * @return Mono of enriched AppContext
     */
    protected Mono<AppContext> enrichContext(AppContext basicContext, 
                                            ServerWebExchange exchange) {
        return Mono.zip(
                resolveRoles(basicContext, exchange),
                resolvePermissions(basicContext, exchange)
        )
        .map(tuple -> basicContext.toBuilder()
                .roles(tuple.getT1())
                .permissions(tuple.getT2())
                .build())
        .defaultIfEmpty(basicContext);
    }
    
    /**
     * Resolves roles for the party in the context of the contract/product.
     * 
     * <p>TODO: Implementation should use common-platform-customer-mgmt-sdk and 
     * common-platform-contract-mgmt-sdk to fetch the party's roles in the contract.</p>
     * 
     * @param context the application context
     * @param exchange the server web exchange
     * @return Mono of role set
     */
    protected Mono<Set<String>> resolveRoles(AppContext context, ServerWebExchange exchange) {
        // TODO: Implement role resolution using platform SDKs
        // Example:
        // return customerManagementClient.getPartyRoles(context.getPartyId(), context.getContractId())
        //     .map(response -> response.getRoles());
        
        log.debug("Resolving roles for party: {} in contract: {}", 
                context.getPartyId(), context.getContractId());
        return Mono.just(Set.of());
    }
    
    /**
     * Resolves permissions for the party in the context of the contract/product.
     * 
     * <p>TODO: Implementation should use common-platform-contract-mgmt-sdk to fetch
     * the party's permissions based on their roles in the contract.</p>
     * 
     * @param context the application context
     * @param exchange the server web exchange
     * @return Mono of permission set
     */
    protected Mono<Set<String>> resolvePermissions(AppContext context, ServerWebExchange exchange) {
        // TODO: Implement permission resolution using platform SDKs
        // Example:
        // return contractManagementClient.getPartyPermissions(
        //     context.getPartyId(), 
        //     context.getContractId(),
        //     context.getProductId()
        // ).map(response -> response.getPermissions());
        
        log.debug("Resolving permissions for party: {} in contract: {}, product: {}", 
                context.getPartyId(), context.getContractId(), context.getProductId());
        return Mono.just(Set.of());
    }
    
    /**
     * Extracts UUID from request attribute or header.
     * 
     * @param exchange the server web exchange
     * @param attributeName the attribute name
     * @param headerName the header name
     * @return Mono of UUID
     */
    protected Mono<UUID> extractUUID(ServerWebExchange exchange, String attributeName, String headerName) {
        // Try to get from attribute first
        UUID fromAttribute = exchange.getAttribute(attributeName);
        if (fromAttribute != null) {
            return Mono.just(fromAttribute);
        }
        
        // Try to get from header
        String headerValue = exchange.getRequest().getHeaders().getFirst(headerName);
        if (headerValue != null && !headerValue.isEmpty()) {
            try {
                return Mono.just(UUID.fromString(headerValue));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID format in header {}: {}", headerName, headerValue);
            }
        }
        
        return Mono.empty();
    }
    
    /**
     * Extracts UUID from path variable.
     * 
     * @param exchange the server web exchange
     * @param variableName the path variable name
     * @return Mono of UUID
     */
    protected Mono<UUID> extractUUIDFromPath(ServerWebExchange exchange, String variableName) {
        try {
            String value = exchange.getRequest().getPath().value();
            // This is a simplified implementation
            // In practice, you'd use a proper path matcher or get it from request attributes
            return Mono.empty();
        } catch (Exception e) {
            log.warn("Failed to extract UUID from path variable: {}", variableName, e);
            return Mono.empty();
        }
    }
}
