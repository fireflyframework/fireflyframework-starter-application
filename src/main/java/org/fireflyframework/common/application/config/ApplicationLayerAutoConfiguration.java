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

import org.fireflyframework.application.actuator.FireflyApplicationInfoContributor;
import org.fireflyframework.application.aop.SecurityAspect;
import org.fireflyframework.application.context.AppMetadata;
import org.fireflyframework.application.health.ApplicationLayerHealthIndicator;
import org.fireflyframework.application.plugin.ProcessPluginRegistry;
import org.fireflyframework.application.plugin.config.PluginProperties;
import org.fireflyframework.application.plugin.metrics.PluginMetricsService;
import org.fireflyframework.application.resolver.ConfigResolver;
import org.fireflyframework.application.resolver.ContextResolver;
import org.fireflyframework.application.resolver.DefaultConfigResolver;
import org.fireflyframework.application.resolver.DefaultContextResolver;
import org.fireflyframework.application.security.DefaultSecurityAuthorizationService;
import org.fireflyframework.application.security.EndpointSecurityRegistry;
import org.fireflyframework.application.security.SecurityAuthorizationService;
import org.fireflyframework.common.application.spi.SessionContext;
import org.fireflyframework.common.application.spi.SessionManager;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

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

    /**
     * Creates the Application Layer health indicator bean.
     *
     * @return ApplicationLayerHealthIndicator instance
     */
    @Bean
    @ConditionalOnMissingBean
    public ApplicationLayerHealthIndicator applicationLayerHealthIndicator() {
        log.info("Creating ApplicationLayerHealthIndicator bean");
        return new ApplicationLayerHealthIndicator();
    }

    /**
     * Creates the security aspect bean for processing @Secure annotations.
     *
     * @param authorizationService the security authorization service
     * @param endpointSecurityRegistry the endpoint security registry
     * @return SecurityAspect instance
     */
    @Bean
    @ConditionalOnMissingBean
    public SecurityAspect securityAspect(SecurityAuthorizationService authorizationService,
                                         EndpointSecurityRegistry endpointSecurityRegistry) {
        log.info("Creating SecurityAspect bean");
        return new SecurityAspect(authorizationService, endpointSecurityRegistry);
    }

    /**
     * Creates the default context resolver bean.
     * Uses {@link ObjectProvider} for the optional {@link SessionManager} dependency.
     *
     * @param sessionManagerProvider optional session manager provider
     * @return DefaultContextResolver instance
     */
    @Bean
    @ConditionalOnMissingBean(ContextResolver.class)
    public DefaultContextResolver defaultContextResolver(
            ObjectProvider<SessionManager<SessionContext>> sessionManagerProvider) {
        log.info("Creating DefaultContextResolver bean");
        return new DefaultContextResolver(sessionManagerProvider.getIfAvailable());
    }

    /**
     * Creates the default config resolver bean.
     *
     * @return DefaultConfigResolver instance
     */
    @Bean
    @ConditionalOnMissingBean(ConfigResolver.class)
    public DefaultConfigResolver defaultConfigResolver() {
        log.info("Creating DefaultConfigResolver bean");
        return new DefaultConfigResolver();
    }

    /**
     * Creates the default security authorization service bean.
     * Uses {@link ObjectProvider} for the optional {@link SessionManager} dependency.
     *
     * @param sessionManagerProvider optional session manager provider
     * @return DefaultSecurityAuthorizationService instance
     */
    @Bean
    @ConditionalOnMissingBean(SecurityAuthorizationService.class)
    public DefaultSecurityAuthorizationService defaultSecurityAuthorizationService(
            ObjectProvider<SessionManager<SessionContext>> sessionManagerProvider) {
        log.info("Creating DefaultSecurityAuthorizationService bean");
        return new DefaultSecurityAuthorizationService(sessionManagerProvider.getIfAvailable());
    }

    /**
     * Creates the Firefly application info contributor bean for the actuator info endpoint.
     *
     * @param appMetadata the application metadata
     * @return FireflyApplicationInfoContributor instance
     */
    @Bean
    @ConditionalOnMissingBean
    public FireflyApplicationInfoContributor fireflyApplicationInfoContributor(AppMetadata appMetadata) {
        log.info("Creating FireflyApplicationInfoContributor bean");
        return new FireflyApplicationInfoContributor(appMetadata);
    }

    /**
     * Creates the process plugin registry bean.
     *
     * @return ProcessPluginRegistry instance
     */
    @Bean
    @ConditionalOnMissingBean
    public ProcessPluginRegistry processPluginRegistry() {
        log.info("Creating ProcessPluginRegistry bean");
        return new ProcessPluginRegistry();
    }

    /**
     * Creates the plugin metrics service bean.
     * Only created when Micrometer's {@link MeterRegistry} is on the classpath.
     *
     * @param meterRegistry the meter registry
     * @param pluginProperties the plugin properties
     * @return PluginMetricsService instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(MeterRegistry.class)
    public PluginMetricsService pluginMetricsService(MeterRegistry meterRegistry,
                                                     PluginProperties pluginProperties) {
        log.info("Creating PluginMetricsService bean");
        return new PluginMetricsService(meterRegistry, pluginProperties);
    }
}
