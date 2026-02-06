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

package org.fireflyframework.application.context;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import jakarta.validation.constraints.NotNull;

/**
 * Complete execution context for an application request.
 * Aggregates all contextual information needed for request processing.
 * 
 * <p>This is the main context object that flows through the application layer,
 * containing all necessary information for:</p>
 * <ul>
 *   <li>Business context and authorization (AppContext)</li>
 *   <li>Tenant configuration and providers (AppConfig)</li>
 *   <li>Security and access control (AppSecurityContext)</li>
 * </ul>
 * 
 * <p><b>Note:</b> Application metadata ({@code @FireflyApplication}) is now application-level (singleton),
 * not per-request, and accessed via {@code AppMetadataProvider}.</p>
 * 
 * <p>Usage example:</p>
 * <pre>
 * ApplicationExecutionContext context = ApplicationExecutionContext.builder()
 *     .context(appContext)
 *     .config(appConfig)
 *     .securityContext(securityContext)
 *     .build();
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
@With
public class ApplicationExecutionContext {
    
    /**
     * Business context (partyId, contractId, productId, roles, permissions)
     */
    @NotNull
    AppContext context;
    
    /**
     * Application configuration (tenantId, providers, feature flags)
     */
    @NotNull
    AppConfig config;
    
    /**
     * Security context (endpoint security, authorization results)
     */
    AppSecurityContext securityContext;
    
    /**
     * Creates a minimal execution context with only required fields
     * 
     * @param partyId the party ID
     * @param tenantId the tenant ID
     * @return a new ApplicationExecutionContext
     */
    public static ApplicationExecutionContext createMinimal(java.util.UUID partyId, java.util.UUID tenantId) {
        return ApplicationExecutionContext.builder()
                .context(AppContext.builder()
                        .partyId(partyId)
                        .tenantId(tenantId)
                        .build())
                .config(AppConfig.builder()
                        .tenantId(tenantId)
                        .build())
                .build();
    }
    
    /**
     * Gets the tenant ID from the config
     * 
     * @return the tenant ID
     */
    public java.util.UUID getTenantId() {
        return config.getTenantId();
    }
    
    /**
     * Gets the party ID from the context
     * 
     * @return the party ID
     */
    public java.util.UUID getPartyId() {
        return context.getPartyId();
    }
    
    /**
     * Gets the contract ID from the context
     * 
     * @return the contract ID (may be null)
     */
    public java.util.UUID getContractId() {
        return context.getContractId();
    }
    
    /**
     * Gets the product ID from the context
     * 
     * @return the product ID (may be null)
     */
    public java.util.UUID getProductId() {
        return context.getProductId();
    }
    
    /**
     * Checks if the context is authorized
     * 
     * @return true if security context exists and is authorized
     */
    public boolean isAuthorized() {
        return securityContext != null && securityContext.isAuthorized();
    }
    
    /**
     * Checks if the context has a specific role
     * 
     * @param role the role to check
     * @return true if the role is present in the context
     */
    public boolean hasRole(String role) {
        return context.hasRole(role);
    }
    
    /**
     * Checks if a feature is enabled for this tenant
     * 
     * @param feature the feature flag name
     * @return true if the feature is enabled
     */
    public boolean isFeatureEnabled(String feature) {
        return config.isFeatureEnabled(feature);
    }
}
