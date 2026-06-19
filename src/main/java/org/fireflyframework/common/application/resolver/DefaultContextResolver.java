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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.spi.SecurityContextPort;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

/**
 * Default {@link ContextResolver} provided by the library: it projects the request context from the
 * <strong>validated</strong> security principal exposed by the {@code fireflyframework-security}
 * platform ({@link SecurityContextPort}). The subject is the principal's subject, roles are its
 * authorities, and permissions are its scopes — no trusted {@code X-Party-Id}-style header is read.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultContextResolver extends AbstractContextResolver {

    private final SecurityContextPort securityContextPort;

    @Override
    public Mono<String> resolveSubject(ServerWebExchange exchange) {
        return securityContextPort.currentPrincipal()
                .map(SecurityPrincipal::subject)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "No authenticated security principal available to resolve the request subject")));
    }

    @Override
    public Mono<UUID> resolveTenantId(ServerWebExchange exchange) {
        return securityContextPort.currentPrincipal()
                .flatMap(principal -> {
                    String tenant = principal.tenantId();
                    if (tenant == null || tenant.isBlank()) {
                        return Mono.empty();
                    }
                    try {
                        return Mono.just(UUID.fromString(tenant));
                    } catch (IllegalArgumentException ex) {
                        log.debug("Principal tenantId '{}' is not a UUID; treating as single-tenant", tenant);
                        return Mono.empty();
                    }
                });
    }

    @Override
    protected Mono<Set<String>> resolveRoles(String subject, ServerWebExchange exchange) {
        return securityContextPort.currentPrincipal()
                .map(SecurityPrincipal::authorities)
                .defaultIfEmpty(Set.of());
    }

    @Override
    protected Mono<Set<String>> resolvePermissions(String subject, ServerWebExchange exchange) {
        return securityContextPort.currentPrincipal()
                .map(SecurityPrincipal::scopes)
                .defaultIfEmpty(Set.of());
    }
}
