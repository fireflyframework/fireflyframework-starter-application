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

package org.fireflyframework.common.application.context;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Complete execution context for an application request: the product-agnostic {@link AppContext},
 * the tenant {@link AppConfig}, and the {@link AppSecurityContext}.
 */
@Value
@Builder(toBuilder = true)
@With
public class ApplicationExecutionContext {

    /** Product-agnostic request context (subject, tenant, roles, permissions, attributes). */
    @NotNull
    AppContext context;

    /** Application/tenant configuration (tenantId, providers, feature flags). */
    @NotNull
    AppConfig config;

    /** Security context (endpoint security, authorization results). */
    AppSecurityContext securityContext;

    /**
     * Creates a minimal execution context with only the required fields.
     *
     * @param subject  the authenticated subject
     * @param tenantId the tenant id
     * @return a new ApplicationExecutionContext
     */
    public static ApplicationExecutionContext createMinimal(String subject, UUID tenantId) {
        return ApplicationExecutionContext.builder()
                .context(AppContext.builder().subject(subject).tenantId(tenantId).build())
                .config(AppConfig.builder().tenantId(tenantId).build())
                .build();
    }

    /** @return the tenant id from the config. */
    public UUID getTenantId() {
        return config.getTenantId();
    }

    /** @return the authenticated subject from the context. */
    public String getSubject() {
        return context.getSubject();
    }

    /** @return true if a security context exists and authorization succeeded. */
    public boolean isAuthorized() {
        return securityContext != null && securityContext.isAuthorized();
    }

    /** @return true if the context holds the given role. */
    public boolean hasRole(String role) {
        return context.hasRole(role);
    }

    /** @return true if the feature is enabled for this tenant. */
    public boolean isFeatureEnabled(String feature) {
        return config.isFeatureEnabled(feature);
    }
}
