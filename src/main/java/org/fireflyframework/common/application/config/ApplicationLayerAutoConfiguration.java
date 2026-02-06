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

package org.fireflyframework.application.config;

import org.fireflyframework.application.security.EndpointSecurityRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for the Application Layer.
 * Automatically registers beans and enables AOP for security annotations.
 * 
 * <p>This configuration is automatically loaded by Spring Boot when the library
 * is on the classpath.</p>
 * 
 * <p>Infrastructure Features Enabled:</p>
 * <ul>
 *   <li>Security AOP (@Secure annotation processing)</li>
 *   <li>Context Resolution (ContextResolver, ConfigResolver)</li>
 *   <li>Caching (via lib-common-cache)</li>
 *   <li>CQRS Support (via lib-common-cqrs)</li>
 *   <li>Event-Driven Architecture (via lib-common-eda)</li>
 *   <li>Health Checks (ApplicationLayerHealthIndicator)</li>
 *   <li>Structured Logging (JSON logging via logback-spring.xml)</li>
 *   <li>Banner (Firefly Application Layer banner)</li>
 * </ul>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(ApplicationLayerProperties.class)
@ComponentScan(basePackages = {
    "org.fireflyframework.application.aop",
    "org.fireflyframework.application.resolver",
    "org.fireflyframework.application.security",
    "org.fireflyframework.application.health"
})
@EnableAspectJAutoProxy
@EnableCaching
@Slf4j
public class ApplicationLayerAutoConfiguration {
    
    public ApplicationLayerAutoConfiguration() {
        log.info("========================================");
        log.info("Initializing Firefly Application Layer");
        log.info("Library: lib-common-application");
        log.info("Layer: Business Process Orchestration");
        log.info("Features: Context, Security, Config, Cache, CQRS, EDA");
        log.info("========================================");
    }
    
    /**
     * Creates the endpoint security registry bean.
     * 
     * @return EndpointSecurityRegistry instance
     */
    @Bean
    @ConditionalOnMissingBean
    public EndpointSecurityRegistry endpointSecurityRegistry() {
        log.info("Creating EndpointSecurityRegistry bean");
        return new EndpointSecurityRegistry();
    }
    
    /**
     * Banner configuration bean.
     * Ensures the Firefly Application Layer banner is displayed.
     * 
     * @return ApplicationLayerBannerConfig instance
     */
    @Bean
    @ConditionalOnProperty(name = "firefly.application.banner.enabled", matchIfMissing = true)
    public ApplicationLayerBannerConfig applicationLayerBannerConfig() {
        log.debug("Enabling Firefly Application Layer banner");
        return new ApplicationLayerBannerConfig();
    }
}
