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

package org.fireflyframework.application.security.annotation;

import java.lang.annotation.*;

/**
 * Annotation to indicate that a method requires specific context components.
 * Used to ensure that required context information (contract, product, etc.) is present.
 * 
 * <p>Usage example:</p>
 * <pre>
 * {@literal @}PostMapping("/transfer")
 * {@literal @}RequireContext(contract = true, product = true)
 * public Mono&lt;Transfer&gt; transfer(@RequestBody TransferRequest request) {
 *     // This method requires both contractId and productId in the context
 * }
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireContext {
    
    /**
     * Whether a contract ID is required in the context.
     * 
     * @return true if contractId must be present
     */
    boolean contract() default false;
    
    /**
     * Whether a product ID is required in the context.
     * 
     * @return true if productId must be present
     */
    boolean product() default false;
    
    /**
     * Whether tenant configuration must be loaded.
     * 
     * @return true if tenant config must be present
     */
    boolean tenantConfig() default true;
    
    /**
     * Specific providers that must be configured for the tenant.
     * 
     * @return array of required provider types
     */
    String[] requiredProviders() default {};
    
    /**
     * Whether to fail fast if context requirements are not met.
     * If true, throw an exception immediately.
     * If false, log a warning and continue.
     * 
     * @return true to fail fast
     */
    boolean failFast() default true;
}
