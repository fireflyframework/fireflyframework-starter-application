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

package org.fireflyframework.application.actuator;

import org.fireflyframework.application.context.AppMetadata;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contributes Firefly application metadata to the {@code /actuator/info} endpoint.
 * 
 * <p>This makes application metadata available for:
 * <ul>
 *   <li>Service discovery and catalog systems</li>
 *   <li>Monitoring dashboards</li>
 *   <li>API gateways and service mesh</li>
 *   <li>Developer tools and CLIs</li>
 * </ul>
 * 
 * <p>Example {@code /actuator/info} response:</p>
 * <pre>
 * {
 *   "firefly": {
 *     "application": {
 *       "name": "customer-onboarding",
 *       "displayName": "Customer Onboarding Service",
 *       "version": "1.0.0",
 *       "description": "Orchestrates customer onboarding...",
 *       "domain": "customer",
 *       "team": "customer-experience",
 *       "owners": ["john.doe@getfirefly.io"],
 *       "apiBasePath": "/api/v1/onboarding",
 *       "usesServices": ["customer-domain-people", "kyc-provider-api"],
 *       "capabilities": ["Customer Identity Verification", "Document Management"],
 *       "environment": "prod",
 *       "startupTime": "2025-01-15T10:30:00Z",
 *       "buildInfo": {
 *         "git.commit": "a1b2c3d",
 *         "git.branch": "main",
 *         "build.time": "2025-01-15T09:00:00Z"
 *       }
 *     }
 *   }
 * }
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see AppMetadata
 */
public class FireflyApplicationInfoContributor implements InfoContributor {
    
    private final AppMetadata appMetadata;
    
    public FireflyApplicationInfoContributor(AppMetadata appMetadata) {
        this.appMetadata = appMetadata;
    }
    
    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> fireflyInfo = new LinkedHashMap<>();
        Map<String, Object> applicationInfo = new LinkedHashMap<>();
        
        // Core identity
        applicationInfo.put("name", appMetadata.getName());
        applicationInfo.put("displayName", appMetadata.getEffectiveDisplayName());
        applicationInfo.put("version", appMetadata.getVersion());
        applicationInfo.put("description", appMetadata.getDescription());
        
        // Architecture
        applicationInfo.put("domain", appMetadata.getDomain());
        
        // Ownership
        applicationInfo.put("team", appMetadata.getTeam());
        if (appMetadata.getOwners() != null && !appMetadata.getOwners().isEmpty()) {
            applicationInfo.put("owners", appMetadata.getOwners());
        }
        
        // API
        if (appMetadata.getApiBasePath() != null && !appMetadata.getApiBasePath().isEmpty()) {
            applicationInfo.put("apiBasePath", appMetadata.getApiBasePath());
        }
        
        // Dependencies
        if (appMetadata.getUsesServices() != null && !appMetadata.getUsesServices().isEmpty()) {
            applicationInfo.put("usesServices", appMetadata.getUsesServices());
        }
        
        // Capabilities
        if (appMetadata.getCapabilities() != null && !appMetadata.getCapabilities().isEmpty()) {
            applicationInfo.put("capabilities", appMetadata.getCapabilities());
        }
        
        // Documentation
        if (appMetadata.getDocumentationUrl() != null && !appMetadata.getDocumentationUrl().isEmpty()) {
            applicationInfo.put("documentationUrl", appMetadata.getDocumentationUrl());
        }
        if (appMetadata.getRepositoryUrl() != null && !appMetadata.getRepositoryUrl().isEmpty()) {
            applicationInfo.put("repositoryUrl", appMetadata.getRepositoryUrl());
        }
        
        // Status
        applicationInfo.put("deprecated", appMetadata.isDeprecated());
        if (appMetadata.isDeprecated() && appMetadata.getDeprecationMessage() != null) {
            applicationInfo.put("deprecationMessage", appMetadata.getDeprecationMessage());
        }
        
        // Runtime
        applicationInfo.put("environment", appMetadata.getEnvironment());
        applicationInfo.put("startupTime", appMetadata.getStartupTime());
        
        // Build info
        if (appMetadata.getBuildInfo() != null && !appMetadata.getBuildInfo().isEmpty()) {
            applicationInfo.put("buildInfo", appMetadata.getBuildInfo());
        }
        
        fireflyInfo.put("application", applicationInfo);
        builder.withDetail("firefly", fireflyInfo);
    }
}
