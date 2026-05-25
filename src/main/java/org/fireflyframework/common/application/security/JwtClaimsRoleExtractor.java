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

package org.fireflyframework.common.application.security;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Extracts roles and permissions from JWT token claims.
 * Supports configurable claim paths for different IDP providers:
 * <ul>
 *   <li>Standard: {@code roles} (flat array)</li>
 *   <li>Keycloak: {@code realm_access.roles} (nested object)</li>
 *   <li>AWS Cognito: {@code cognito:groups} (flat array)</li>
 * </ul>
 *
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
public class JwtClaimsRoleExtractor {

    private final String rolesClaimPath;
    private final String permissionsClaimPath;

    public JwtClaimsRoleExtractor(String rolesClaimPath, String permissionsClaimPath) {
        this.rolesClaimPath = rolesClaimPath != null ? rolesClaimPath : "roles";
        this.permissionsClaimPath = permissionsClaimPath != null ? permissionsClaimPath : "permissions";
    }

    /**
     * Extracts roles from JWT claims map.
     *
     * @param claims the JWT claims as a map
     * @return set of role names
     */
    public Set<String> extractRoles(Map<String, Object> claims) {
        return extractValues(claims, rolesClaimPath);
    }

    /**
     * Extracts permissions from JWT claims map.
     *
     * @param claims the JWT claims as a map
     * @return set of permission names
     */
    public Set<String> extractPermissions(Map<String, Object> claims) {
        return extractValues(claims, permissionsClaimPath);
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractValues(Map<String, Object> claims, String claimPath) {
        if (claims == null || claimPath == null || claimPath.isBlank()) {
            return Set.of();
        }

        try {
            String[] pathSegments = claimPath.split("\\.");
            Object current = claims;

            for (String segment : pathSegments) {
                if (current instanceof Map) {
                    current = ((Map<String, Object>) current).get(segment);
                } else {
                    log.debug("Cannot traverse claim path '{}': intermediate value is not a Map", claimPath);
                    return Set.of();
                }
                if (current == null) {
                    return Set.of();
                }
            }

            if (current instanceof Collection) {
                Set<String> result = new HashSet<>();
                for (Object item : (Collection<?>) current) {
                    if (item != null) {
                        result.add(item.toString());
                    }
                }
                return result;
            } else if (current instanceof String) {
                // Single value claim
                return Set.of((String) current);
            }

            log.debug("Claim at path '{}' is not a collection or string: {}", claimPath, current.getClass());
            return Set.of();
        } catch (Exception e) {
            log.warn("Failed to extract values from claim path '{}': {}", claimPath, e.getMessage());
            return Set.of();
        }
    }
}
