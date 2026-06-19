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

package org.fireflyframework.common.application.security.annotation;

import java.lang.annotation.*;

/**
 * Annotation for declarative endpoint security configuration.
 * Can be applied to controller classes or individual methods.
 *
 * <p>When applied at class level, security rules apply to all methods in the class.
 * Method-level annotations override class-level security configuration.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * {@literal @}RestController
 * {@literal @}RequestMapping("/api/v1/resources")
 * {@literal @}Secure(roles = {"RESOURCE_OWNER", "RESOURCE_ADMIN"})
 * public class ResourceController {
 *
 *     {@literal @}GetMapping("/{id}")
 *     public Mono&lt;Resource&gt; getResource(@PathVariable UUID id) {
 *         // Only accessible by users with RESOURCE_OWNER or RESOURCE_ADMIN roles
 *     }
 *
 *     {@literal @}PostMapping("/{id}/action")
 *     {@literal @}Secure(roles = "RESOURCE_OWNER", permissions = "EXECUTE_ACTION")
 *     public Mono&lt;ActionResult&gt; action(@PathVariable UUID id, @RequestBody ActionRequest request) {
 *         // Requires both RESOURCE_OWNER role AND EXECUTE_ACTION permission
 *     }
 *
 *     {@literal @}GetMapping("/public")
 *     {@literal @}Secure(allowAnonymous = true)
 *     public Mono&lt;List&lt;Resource&gt;&gt; listPublicResources() {
 *         // Accessible without authentication
 *     }
 * }
 * </pre>
 *
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Secure {

    /**
     * Roles required to access this endpoint.
     * If multiple roles are specified, the user must have at least one of them (OR logic).
     *
     * @return array of required role names
     */
    String[] roles() default {};

    /**
     * Permissions required to access this endpoint.
     * If multiple permissions are specified, the user must have at least one of them (OR logic).
     *
     * @return array of required permission names
     */
    String[] permissions() default {};

    /**
     * Whether all specified roles are required (AND logic) instead of any (OR logic).
     *
     * @return true to require all roles, false to require any role
     */
    boolean requireAllRoles() default false;

    /**
     * Whether all specified permissions are required (AND logic) instead of any (OR logic).
     *
     * @return true to require all permissions, false to require any permission
     */
    boolean requireAllPermissions() default false;

    /**
     * Whether anonymous access is allowed.
     * If true, no authentication is required.
     *
     * @return true to allow anonymous access
     */
    boolean allowAnonymous() default false;

    /**
     * Whether authentication is required.
     * If false, the endpoint can be accessed without authentication.
     *
     * @return true if authentication is required
     */
    boolean requiresAuthentication() default true;

    /**
     * Custom security expression using SpEL.
     * Allows for complex authorization logic beyond simple role/permission checks.
     *
     * <p>Available variables in expressions:</p>
     * <ul>
     *   <li>context: The current AppContext</li>
     *   <li>config: The current AppConfig</li>
     *   <li>metadata: The current AppMetadata</li>
     *   <li>principal: The authenticated principal</li>
     * </ul>
     *
     * <p>Example: {@code expression = "context.hasRole('ADMIN') or context.subject == #ownerId"}</p>
     *
     * @return SpEL expression for custom authorization logic
     */
    String expression() default "";

    /**
     * Additional security attributes as key=value pairs.
     * Can be used for custom security extensions.
     *
     * <p>Example: {@code attributes = {"rateLimit=100", "ipWhitelist=true"}}</p>
     *
     * @return array of key=value security attribute pairs
     */
    String[] attributes() default {};

    /**
     * Description of the security requirement for documentation purposes.
     *
     * @return human-readable description of security requirements
     */
    String description() default "";
}
