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
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;

/**
 * Template base for {@link ContextResolver}s: assembles an {@link AppContext} from the resolved
 * subject, tenant, roles and permissions. Subclasses provide {@link #resolveSubject} and
 * {@link #resolveTenantId}; roles/permissions default to empty and can be overridden.
 */
@Slf4j
public abstract class AbstractContextResolver implements ContextResolver {

    @Override
    public Mono<AppContext> resolveContext(ServerWebExchange exchange) {
        return resolveSubject(exchange).flatMap(subject -> Mono.zip(
                        resolveTenantId(exchange).map(Optional::of).defaultIfEmpty(Optional.empty()),
                        resolveRoles(subject, exchange),
                        resolvePermissions(subject, exchange))
                .map(tuple -> AppContext.builder()
                        .subject(subject)
                        .tenantId(tuple.getT1().orElse(null))
                        .roles(tuple.getT2())
                        .permissions(tuple.getT3())
                        .build()))
                .doOnError(error -> log.error("Failed to resolve application context", error));
    }

    /** Resolve roles for the subject (default empty; override to enrich). */
    protected Mono<Set<String>> resolveRoles(String subject, ServerWebExchange exchange) {
        return Mono.just(Set.of());
    }

    /** Resolve permissions for the subject (default empty; override to enrich). */
    protected Mono<Set<String>> resolvePermissions(String subject, ServerWebExchange exchange) {
        return Mono.just(Set.of());
    }
}
