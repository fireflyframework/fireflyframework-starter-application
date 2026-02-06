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
import java.util.Set;
import java.util.UUID;

/**
 * Immutable business context container for application requests.
 * Contains information about the party (customer), contract, and product involved in the request.
 * 
 * <p>This class represents the "who", "what", and "where" of a business operation:
 * <ul>
 *   <li><strong>partyId</strong>: Who is making the request (customer/user)</li>
 *   <li><strong>contractId</strong>: What contract/agreement is involved</li>
 *   <li><strong>productId</strong>: What product is being accessed/modified</li>
 * </ul>
 * </p>
 * 
 * <p>This context is used for authorization decisions and domain logic execution.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Value
@Builder(toBuilder = true)
@With
public class AppContext {
    
    /**
     * Unique identifier of the party (customer) making the request.
     * This comes from common-platform-customer-mgmt.
     */
    @NotNull
    UUID partyId;
    
    /**
     * Unique identifier of the contract associated with this request.
     * This comes from common-platform-contract-mgmt.
     * Optional for operations that don't require a contract context.
     */
    UUID contractId;
    
    /**
     * Unique identifier of the product being accessed or modified.
     * This comes from common-platform-product-mgmt.
     * Optional for operations that don't require a product context.
     */
    UUID productId;
    
    /**
     * Roles that the party has in the context of this contract/product.
     * Used for authorization decisions.
     */
    Set<String> roles;
    
    /**
     * Permissions that the party has in this context.
     * Derived from roles and used for fine-grained authorization.
     */
    Set<String> permissions;
    
    /**
     * The tenant/organization this context belongs to.
     * Links to AppConfig's tenantId.
     */
    UUID tenantId;
    
    /**
     * Additional context-specific attributes.
     * Can be used to store domain-specific context information.
     */
    java.util.Map<String, Object> attributes;
    
    /**
     * Checks if the context has a specific role
     * 
     * @param role the role to check
     * @return true if the role is present
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    /**
     * Checks if the context has any of the specified roles
     * 
     * @param roles the roles to check
     * @return true if any of the roles are present
     */
    public boolean hasAnyRole(String... roles) {
        if (this.roles == null || roles == null) {
            return false;
        }
        for (String role : roles) {
            if (this.roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if the context has all of the specified roles
     * 
     * @param roles the roles to check
     * @return true if all roles are present
     */
    public boolean hasAllRoles(String... roles) {
        if (this.roles == null || roles == null) {
            return false;
        }
        for (String role : roles) {
            if (!this.roles.contains(role)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if the context has a specific permission
     * 
     * @param permission the permission to check
     * @return true if the permission is present
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
    
    /**
     * Checks if this context has a contract association
     * 
     * @return true if contractId is present
     */
    public boolean hasContract() {
        return contractId != null;
    }
    
    /**
     * Checks if this context has a product association
     * 
     * @return true if productId is present
     */
    public boolean hasProduct() {
        return productId != null;
    }
    
    /**
     * Gets an attribute from the context
     * 
     * @param key the attribute key
     * @param <T> the expected type
     * @return the attribute value or null if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return attributes != null ? (T) attributes.get(key) : null;
    }
}
