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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable, product-agnostic request context. It carries the authenticated {@code subject}, an
 * optional generic {@code tenantId}, the granted {@code roles}/{@code permissions}, and an open
 * {@code attributes} map. It deliberately holds <strong>no</strong> product-domain concepts —
 * products carry their own model (e.g. party/contract/product references) inside {@link #attributes}.
 *
 * <p>The {@code subject} and authorities are a projection of the validated security principal
 * (see the {@code fireflyframework-security} platform), not of any trusted transport header.
 */
@Value
@Builder(toBuilder = true)
@With
public class AppContext {

    /** Authenticated subject identifier (OIDC {@code sub}); from the validated security principal. */
    String subject;

    /** Generic tenant discriminator; {@code null} when single-tenant. */
    UUID tenantId;

    /** Granted authorities/roles, used for authorization decisions. */
    Set<String> roles;

    /** Granted fine-grained permissions/scopes. */
    Set<String> permissions;

    /** Open, product-defined attributes (also the ABAC input bag). */
    Map<String, Object> attributes;

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(String... candidates) {
        if (roles == null || candidates == null) {
            return false;
        }
        for (String role : candidates) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllRoles(String... candidates) {
        if (roles == null || candidates == null) {
            return false;
        }
        for (String role : candidates) {
            if (!roles.contains(role)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return attributes != null ? (T) attributes.get(key) : null;
    }
}
