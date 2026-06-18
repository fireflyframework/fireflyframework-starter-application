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

package org.fireflyframework.common.application.resolver;

import org.fireflyframework.common.application.context.AppContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Resolves the product-agnostic {@link AppContext} for a request from the validated security
 * context. Implementations derive the subject, tenant, roles and permissions — never from a trusted
 * transport header.
 */
public interface ContextResolver {

    /** Resolve the full context (subject + tenant + roles + permissions). */
    Mono<AppContext> resolveContext(ServerWebExchange exchange);

    /** Resolve the authenticated subject identifier. */
    Mono<String> resolveSubject(ServerWebExchange exchange);

    /** Resolve the generic tenant id (may be empty when single-tenant). */
    Mono<UUID> resolveTenantId(ServerWebExchange exchange);

    /** Whether this resolver supports the given request. */
    default boolean supports(ServerWebExchange exchange) {
        return true;
    }

    /** Priority of this resolver (higher wins) when multiple support a request. */
    default int getPriority() {
        return 0;
    }
}
