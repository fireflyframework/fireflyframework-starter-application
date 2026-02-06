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

package org.fireflyframework.application.metadata;

import org.fireflyframework.application.context.AppMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.time.Instant;
import java.util.*;

/**
 * Reads the {@code @FireflyApplication} annotation from the main application class
 * and provides {@link AppMetadata} as a singleton Spring bean.
 * 
 * <p>This provider automatically:
 * <ul>
 *   <li>Scans for @FireflyApplication annotation on Spring Boot main class</li>
 *   <li>Builds AppMetadata from annotation values</li>
 *   <li>Enriches with runtime information (startup time, environment, build info)</li>
 *   <li>Exposes AppMetadata bean for injection anywhere in the application</li>
 *   <li>Logs application startup banner with metadata</li>
 * </ul>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see FireflyApplication
 * @see AppMetadata
 */
@Configuration
@Slf4j
public class ApplicationMetadataProvider implements ApplicationListener<ApplicationReadyEvent> {
    
    private final Environment environment;
    private final AppMetadata appMetadata;
    
    public ApplicationMetadataProvider(ApplicationContext applicationContext, Environment environment) {
        this.environment = environment;
        this.appMetadata = buildAppMetadata(applicationContext, environment);
    }
    
    /**
     * Provides the AppMetadata bean.
     *
     * @return the application metadata
     */
    @Bean
    public AppMetadata appMetadata() {
        return appMetadata;
    }
    
    /**
     * Logs application startup banner when application is ready.
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        logApplicationBanner();
    }
    
    /**
     * Builds AppMetadata from @FireflyApplication annotation.
     */
    private AppMetadata buildAppMetadata(ApplicationContext applicationContext, Environment environment) {
        // Find the @FireflyApplication annotation
        FireflyApplication annotation = findFireflyApplicationAnnotation(applicationContext);
        
        if (annotation == null) {
            log.warn("No @FireflyApplication annotation found. Using default application metadata.");
            return buildDefaultMetadata(environment);
        }
        
        // Build metadata from annotation
        return AppMetadata.builder()
                .name(annotation.name())
                .displayName(annotation.displayName().isEmpty() ? annotation.name() : annotation.displayName())
                .description(annotation.description())
                .version(environment.getProperty("spring.application.version", "unknown"))
                .domain(annotation.domain())
                .team(annotation.team())
                .owners(new HashSet<>(Arrays.asList(annotation.owners())))
                .apiBasePath(annotation.apiBasePath())
                .usesServices(new HashSet<>(Arrays.asList(annotation.usesServices())))
                .capabilities(new HashSet<>(Arrays.asList(annotation.capabilities())))
                .documentationUrl(annotation.documentationUrl())
                .repositoryUrl(annotation.repositoryUrl())
                .deprecated(annotation.deprecated())
                .deprecationMessage(annotation.deprecationMessage())
                .startupTime(Instant.now())
                .environment(resolveEnvironment(environment))
                .buildInfo(extractBuildInfo(environment))
                .customProperties(new HashMap<>())
                .build();
    }
    
    /**
     * Finds @FireflyApplication annotation in the application context.
     */
    private FireflyApplication findFireflyApplicationAnnotation(ApplicationContext applicationContext) {
        // Get all bean names
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        
        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                Class<?> beanClass = bean.getClass();
                
                // Check if class has @FireflyApplication
                FireflyApplication annotation = beanClass.getAnnotation(FireflyApplication.class);
                if (annotation != null) {
                    log.debug("Found @FireflyApplication on class: {}", beanClass.getName());
                    return annotation;
                }
                
                // Check superclass (for proxied beans)
                Class<?> superclass = beanClass.getSuperclass();
                if (superclass != null) {
                    annotation = superclass.getAnnotation(FireflyApplication.class);
                    if (annotation != null) {
                        log.debug("Found @FireflyApplication on superclass: {}", superclass.getName());
                        return annotation;
                    }
                }
            } catch (Exception e) {
                // Ignore beans that can't be retrieved
            }
        }
        
        return null;
    }
    
    /**
     * Builds default metadata when no annotation is present.
     */
    private AppMetadata buildDefaultMetadata(Environment environment) {
        return AppMetadata.builder()
                .name(environment.getProperty("spring.application.name", "unknown-application"))
                .version(environment.getProperty("spring.application.version", "unknown"))
                .description("Firefly Application (no @FireflyApplication annotation found)")
                .domain("unknown")
                .team("unknown")
                .owners(new HashSet<>())
                .startupTime(Instant.now())
                .environment(resolveEnvironment(environment))
                .buildInfo(extractBuildInfo(environment))
                .customProperties(new HashMap<>())
                .build();
    }
    
    /**
     * Resolves the environment name from Spring profiles.
     */
    private String resolveEnvironment(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            return activeProfiles[0];
        }
        
        String[] defaultProfiles = environment.getDefaultProfiles();
        if (defaultProfiles.length > 0 && !defaultProfiles[0].equals("default")) {
            return defaultProfiles[0];
        }
        
        return "unknown";
    }
    
    /**
     * Extracts build information from environment properties.
     */
    private Map<String, String> extractBuildInfo(Environment environment) {
        Map<String, String> buildInfo = new HashMap<>();
        
        // Standard Spring Boot Actuator build info properties
        addIfPresent(buildInfo, "git.commit", environment);
        addIfPresent(buildInfo, "git.branch", environment);
        addIfPresent(buildInfo, "git.commit.time", environment);
        addIfPresent(buildInfo, "build.time", environment);
        addIfPresent(buildInfo, "build.version", environment);
        addIfPresent(buildInfo, "build.artifact", environment);
        addIfPresent(buildInfo, "build.group", environment);
        addIfPresent(buildInfo, "build.name", environment);
        
        return buildInfo;
    }
    
    /**
     * Adds property to map if it exists in environment.
     */
    private void addIfPresent(Map<String, String> map, String key, Environment environment) {
        String value = environment.getProperty(key);
        if (value != null) {
            map.put(key, value);
        }
    }
    
    /**
     * Logs a banner with application metadata when startup is complete.
     */
    private void logApplicationBanner() {
        log.info("\n" +
                "========================================================================================================\n" +
                "  Firefly Application Started\n" +
                "========================================================================================================\n" +
                "  Name:          {}\n" +
                "  Version:       {}\n" +
                "  Domain:        {}\n" +
                "  Team:          {}\n" +
                "  Environment:   {}\n" +
                "  API Base:      {}\n" +
                "  Started:       {}\n" +
                "  Description:   {}\n" +
                "========================================================================================================",
                appMetadata.getEffectiveDisplayName(),
                appMetadata.getVersion(),
                appMetadata.getDomain(),
                appMetadata.getTeam(),
                appMetadata.getEnvironment(),
                appMetadata.getApiBasePath() != null ? appMetadata.getApiBasePath() : "N/A",
                appMetadata.getStartupTime(),
                appMetadata.getDescription()
        );
        
        if (appMetadata.isDeprecated()) {
            log.warn("\n" +
                    "⚠️  WARNING: This application is DEPRECATED\n" +
                    "   {}\n",
                    appMetadata.getDeprecationMessage()
            );
        }
    }
}
