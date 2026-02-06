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

package org.fireflyframework.application.resolver;

import org.fireflyframework.application.context.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Default implementation of ConfigResolver.
 * 
 * <p><strong>This is provided by the library - microservices don't need to implement anything.</strong></p>
 * 
 * <p>This resolver automatically:</p>
 * <ul>
 *   <li>Fetches tenant configuration from platform config-mgmt service</li>
 *   <li>Loads provider configurations (KYC, payment gateways, etc.)</li>
 *   <li>Retrieves feature flags</li>
 *   <li>Caches configuration for performance</li>
 * </ul>
 * 
 * <h2>What Microservices Need to Do</h2>
 * <p><strong>Nothing.</strong> Configuration is loaded automatically.</p>
 * 
 * <h2>Configuration Sources</h2>
 * <p>The resolver fetches configuration from:</p>
 * <ol>
 *   <li><code>common-platform-config-mgmt</code> service (primary)</li>
 *   <li>Local Spring properties (fallback)</li>
 * </ol>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Component
@Slf4j
public class DefaultConfigResolver extends AbstractConfigResolver {
    
    // TODO: Inject platform SDK client when available
    // private final ConfigManagementClient configMgmtClient;
    
    @Override
    protected Mono<AppConfig> fetchConfigFromPlatform(UUID tenantId) {
        log.debug("Fetching configuration for tenant: {}", tenantId);
        
        // TODO: Implement using common-platform-config-mgmt-sdk
        // When SDK is available, call:
        /*
        return configMgmtClient.getTenantConfiguration(tenantId)
            .map(response -> AppConfig.builder()
                .tenantId(response.getTenantId())
                .tenantName(response.getTenantName())
                .providers(convertProviders(response.getProviders()))
                .featureFlags(response.getFeatureFlags())
                .settings(response.getSettings())
                .build())
            .doOnNext(config -> log.info("Loaded configuration for tenant: {} with {} providers, {} feature flags",
                tenantId, 
                config.getProviders().size(),
                config.getFeatureFlags().size()));
        */
        
        // Temporary: Return minimal config until SDK integration is complete
        log.warn("SDK integration pending: returning minimal configuration for tenant: {}", tenantId);
        
        return Mono.just(AppConfig.builder()
            .tenantId(tenantId)
            .tenantName("default-tenant")
            .build());
    }
    
    // TODO: Helper method to convert SDK provider config to library model
    /*
    private Map<String, AppConfig.ProviderConfig> convertProviders(
            List<org.fireflyframework.common.application.spi.ProviderConfiguration> sdkProviders) {
        
        return sdkProviders.stream()
            .collect(Collectors.toMap(
                org.fireflyframework.common.application.spi.ProviderConfiguration::getProviderType,
                provider -> AppConfig.ProviderConfig.builder()
                    .providerType(provider.getProviderType())
                    .providerId(provider.getProviderId())
                    .enabled(provider.isEnabled())
                    .priority(provider.getPriority())
                    .properties(provider.getProperties())
                    .build()
            ));
    }
    */
}
