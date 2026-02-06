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

package org.fireflyframework.application.context;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Immutable metadata about the Firefly application/microservice.
 * 
 * <p>This class contains static, application-level metadata (as opposed to per-request metadata).
 * It is populated from the {@code @FireflyApplication} annotation and provides information about:
 * <ul>
 *   <li>What the application is (name, description, version)</li>
 *   <li>Where it fits in the architecture (domain, business capabilities)</li>
 *   <li>Who is responsible for it (team, owners)</li>
 *   <li>How to find more information (docs, repository)</li>
 *   <li>Build and runtime information (git commit, build time, startup time)</li>
 * </ul>
 * 
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li><b>Service Discovery</b> - Catalog all microservices with their metadata</li>
 *   <li><b>Monitoring</b> - Display application info in dashboards and /actuator/info</li>
 *   <li><b>Governance</b> - Track ownership, teams, and domains</li>
 *   <li><b>Documentation</b> - Auto-generate service catalogs and architecture diagrams</li>
 *   <li><b>Dependency Mapping</b> - Know which services depend on which</li>
 *   <li><b>Developer Experience</b> - Quick understanding of any microservice</li>
 * </ul>
 * 
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // 1. Declare on main class
 * &#64;FireflyApplication(
 *     name = "customer-onboarding",
 *     displayName = "Customer Onboarding Service",
 *     description = "Orchestrates customer onboarding: KYC, document upload, account setup",
 *     domain = "customer",
 *     team = "customer-experience",
 *     owners = {"john.doe@getfirefly.io"},
 *     apiBasePath = "/api/v1/onboarding",
 *     usesServices = {"customer-domain-people", "kyc-provider-api"},
 *     capabilities = {"Identity Verification", "Document Management"}
 * )
 * &#64;SpringBootApplication
 * public class CustomerOnboardingApplication { ... }
 * 
 * // 2. Inject and use anywhere
 * &#64;Service
 * public class MyService {
 *     private final AppMetadata appMetadata;
 *     
 *     public void logApplicationInfo() {
 *         log.info("Service: {} [{}] - Domain: {}", 
 *             appMetadata.getEffectiveDisplayName(),
 *             appMetadata.getVersion(),
 *             appMetadata.getDomain());
 *     }
 * }
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see org.fireflyframework.application.metadata.FireflyApplication
 */
@Value
@Builder(toBuilder = true)
@With
public class AppMetadata {
    
    // ==================== Core Identity ====================
    
    /**
     * The unique name/identifier of the application.
     * Example: "customer-onboarding", "loan-origination"
     */
    String name;
    
    /**
     * The application version (semantic versioning).
     * Example: "1.0.0", "2.1.3-SNAPSHOT"
     */
    String version;
    
    /**
     * A concise description of what this application does.
     * Example: "Customer onboarding orchestration service - KYC, documentation, and account setup"
     */
    String description;
    
    // ==================== Architecture ====================
    
    /**
     * Human-readable display name (optional); falls back to name when empty.
     */
    String displayName;
    
    /**
     * The primary business domain.
     * Example: "customer", "lending", "payment"
     */
    String domain;
    
    
    // ==================== Ownership & Governance ====================
    
    /**
     * The team responsible for this application.
     * Example: "customer-experience", "lending-platform"
     */
    String team;
    
    /**
     * Email addresses of the primary owners/maintainers.
     * Example: ["john.doe@getfirefly.io", "jane.smith@getfirefly.io"]
     */
    Set<String> owners;
    
    /**
     * The base path for the REST API exposed by this application.
     * Example: "/api/v1/onboarding"
     */
    String apiBasePath;
    
    /**
     * Downstream services that this application depends on.
     * Example: ["customer-domain-people", "kyc-provider-api"]
     */
    Set<String> usesServices;
    
    /**
     * Business capabilities that this application provides.
     * Example: ["Customer Identity Verification", "Document Management"]
     */
    Set<String> capabilities;
    
    // ==================== Documentation & Links ====================
    
    /**
     * URL to the application's documentation or wiki.
     * Example: "https://wiki.firefly.com/customer-onboarding"
     */
    String documentationUrl;
    
    /**
     * URL to the application's source code repository.
     * Example: "https://github.com/firefly/customer-application-onboarding"
     */
    String repositoryUrl;
    
    // ==================== Status ====================
    
    /**
     * Whether this application is critical for business operations.
     * Critical apps require higher SLA and 24/7 support.
     */
    boolean critical;
    
    /**
     * Whether this application is deprecated.
     */
    boolean deprecated;
    
    /**
     * Deprecation message explaining why and what to use instead.
     * Example: "Replaced by customer-application-onboarding-v2. Migrate by Q2 2025."
     */
    String deprecationMessage;
    
    // ==================== Runtime & Build Information ====================
    
    /**
     * When the application started.
     */
    Instant startupTime;
    
    /**
     * The runtime environment.
     * Example: "dev", "staging", "prod"
     */
    String environment;
    
    /**
     * Build information (git commit, build time, CI job, etc.)
     * Example: {"git.commit": "a1b2c3d", "git.branch": "main", "build.time": "2025-01-15T10:30:00Z"}
     */
    Map<String, String> buildInfo;
    
    /**
     * Custom properties for extensibility.
     * Can be used for organization-specific metadata.
     */
    Map<String, Object> customProperties;
    
    // ==================== Utility Methods ====================
    
    /**
     * Gets a display name (displayName if provided, otherwise name).
     * 
     * @return display-friendly name
     */
    public String getEffectiveDisplayName() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : name;
    }
    
    
    /**
     * Checks if this is a production environment.
     * 
     * @return true if environment is "prod" or "production"
     */
    public boolean isProduction() {
        return environment != null && 
               (environment.equalsIgnoreCase("prod") || environment.equalsIgnoreCase("production"));
    }
    
    /**
     * Gets build information by key.
     * 
     * @param key the build info key
     * @return the value or null if not present
     */
    public String getBuildInfo(String key) {
        return buildInfo != null ? buildInfo.get(key) : null;
    }
    
    /**
     * Gets a custom property by key.
     * 
     * @param key the property key
     * @return the value or null if not present
     */
    public Object getCustomProperty(String key) {
        return customProperties != null ? customProperties.get(key) : null;
    }
    
    /**
     * Creates a display-friendly summary of this application.
     * 
     * @return formatted summary string
     */
    public String toDisplayString() {
        return String.format("%s v%s [%s] - %s",
            getEffectiveDisplayName(),
            version,
            domain,
            description != null ? description : "No description"
        );
    }
}
