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

package org.fireflyframework.application.plugin.resolver;

import org.fireflyframework.application.plugin.config.PluginProperties;
import org.fireflyframework.application.plugin.service.ProcessMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for process mapping resolution with caching.
 * 
 * <p>This configuration creates a {@link CachingProcessMappingService} that
 * integrates with common-platform-config-mgmt for resolving API→process mappings.</p>
 * 
 * <h3>Prerequisites</h3>
 * <ul>
 *   <li>common-platform-config-mgmt service must be available</li>
 *   <li>config-mgmt URL configured via {@code firefly.config-mgmt.base-url}</li>
 * </ul>
 * 
 * <h3>Configuration</h3>
 * <pre>
 * firefly:
 *   config-mgmt:
 *     base-url: http://config-mgmt-service:8080
 *   application:
 *     plugin:
 *       cache:
 *         enabled: true
 *         ttl: PT1H
 *         max-entries: 1000
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@ConditionalOnProperty(name = "firefly.application.plugin.enabled", havingValue = "true", matchIfMissing = true)
public class ProcessMappingResolverAutoConfiguration {
    
    @Value("${firefly.config-mgmt.base-url:http://localhost:8080}")
    private String configMgmtBaseUrl;
    
    /**
     * Creates a WebClient for communicating with config-mgmt.
     */
    @Bean
    @ConditionalOnMissingBean(name = "configMgmtWebClient")
    public WebClient configMgmtWebClient(WebClient.Builder builder) {
        log.info("Creating config-mgmt WebClient: {}", configMgmtBaseUrl);
        return builder
                .baseUrl(configMgmtBaseUrl)
                .build();
    }
    
    /**
     * Creates the CachingProcessMappingService bean.
     * 
     * <p>This overrides the default ProcessMappingService from PluginAutoConfiguration
     * when config-mgmt integration is available.</p>
     */
    @Bean
    @ConditionalOnProperty(name = "firefly.config-mgmt.enabled", havingValue = "true", matchIfMissing = true)
    public ProcessMappingService cachingProcessMappingService(
            WebClient configMgmtWebClient,
            PluginProperties properties) {
        
        log.info("Creating CachingProcessMappingService with config-mgmt integration");
        return new CachingProcessMappingService(configMgmtWebClient, properties);
    }
}
