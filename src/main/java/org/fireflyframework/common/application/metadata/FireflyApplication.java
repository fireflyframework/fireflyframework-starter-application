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

import org.springframework.stereotype.Indexed;

import java.lang.annotation.*;

/**
 * Declares metadata for a Firefly application/microservice.
 * 
 * <p>This annotation should be placed on the main Spring Boot application class
 * alongside {@code @SpringBootApplication}. It provides essential metadata about
 * the microservice for documentation, service discovery, monitoring, and governance.</p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * &#64;FireflyApplication(
 *     name = "customer-onboarding",
 *     displayName = "Customer Onboarding Service",
 *     description = "Orchestrates customer onboarding: KYC verification, document upload, and account setup",
 *     domain = "customer",
 *     team = "customer-experience",
 *     owners = {"john.doe@getfirefly.io", "jane.smith@getfirefly.io"},
 *     apiBasePath = "/api/v1/onboarding",
 *     usesServices = {"customer-domain-people", "common-platform-customer-mgmt", "kyc-provider-api"}
 * )
 * &#64;SpringBootApplication
 * public class CustomerOnboardingApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(CustomerOnboardingApplication.class, args);
 *     }
 * }
 * </pre>
 * 
 * <h3>Benefits:</h3>
 * <ul>
 *   <li><b>Self-Documentation</b> - Clear metadata about what the service does</li>
 *   <li><b>Service Discovery</b> - Automated service catalog generation</li>
 *   <li><b>Monitoring</b> - Exposed via /actuator/info endpoint</li>
 *   <li><b>Governance</b> - Track ownership, teams, and architectural layer</li>
 *   <li><b>Developer Experience</b> - Quick understanding of any microservice</li>
 * </ul>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 * @see AppMetadata
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Indexed
public @interface FireflyApplication {
    
    /**
     * The unique name/identifier of the application.
     * 
     * <p>Short, kebab-case name without redundant "application" prefix.</p>
     * <p>Examples: "customer-onboarding", "loan-origination", "payment-transfer"</p>
     * 
     * @return the application name (required)
     */
    String name();
    
    /**
     * Human-readable display name of the application.
     * 
     * <p>Used in UIs, dashboards, and service catalogs.</p>
     * <p>Examples: "Customer Onboarding Service", "Loan Origination Workflow"</p>
     * 
     * @return the display name (optional, defaults to name)
     */
    String displayName() default "";
    
    /**
     * Concise description of what this application does and its main responsibilities.
     * 
     * <p>Should be 1-2 sentences explaining the business purpose.</p>
     * <p>Example: "Orchestrates the complete customer onboarding process including KYC verification, 
     * document upload, compliance checks, and initial account setup."</p>
     * 
     * @return the description (required)
     */
    String description();
    
    /**
     * The base path for the REST API exposed by this application.
     * 
     * <p>Used for API documentation, gateway routing, and service mesh configuration.</p>
     * <p>Examples: "/api/v1/onboarding", "/api/v2/loans", "/api/v1/payments"</p>
     * 
     * @return the API base path (optional)
     */
    String apiBasePath() default "";
    
    /**
     * The primary business domain/bounded context this application belongs to.
     * 
     * <p>Used for organizing services and generating architecture diagrams.</p>
     * <p>Examples: "customer", "lending", "payment", "account", "risk", "compliance"</p>
     * 
     * @return the domain (required)
     */
    String domain();
    
    
    /**
     * The team responsible for this application.
     * 
     * <p>Examples: "customer-experience", "lending-platform", "platform-engineering"</p>
     * 
     * @return the team name (required)
     */
    String team();
    
    /**
     * Email addresses of the primary owners/maintainers of this application.
     * 
     * <p>These are the people responsible for the application's development and maintenance.</p>
     * 
     * @return array of owner email addresses (required, at least one)
     */
    String[] owners();
    
    /**
     * Downstream services that this application depends on.
     * 
     * <p>List the names of domain services, platform services, and external APIs 
     * that this application orchestrates or integrates with.</p>
     * 
     * <p>Useful for:</p>
     * <ul>
     *   <li>Dependency mapping and architecture diagrams</li>
     *   <li>Impact analysis for changes</li>
     *   <li>Service mesh configuration</li>
     * </ul>
     * 
     * <p>Examples: {"customer-domain-people", "kyc-provider-api", "common-platform-customer-mgmt"}</p>
     * 
     * @return array of service names this app depends on (optional)
     */
    String[] usesServices() default {};
    
    /**
     * URL to the application's documentation or wiki.
     * 
     * <p>Can be a link to:</p>
     * <ul>
     *   <li>Confluence/Notion page</li>
     *   <li>README in repository</li>
     *   <li>API documentation site</li>
     *   <li>Architecture decision records</li>
     * </ul>
     * 
     * @return the documentation URL (optional)
     */
    String documentationUrl() default "";
    
    /**
     * URL to the application's source code repository.
     * 
     * <p>Examples: "https://github.com/firefly/customer-application-onboarding"</p>
     * 
     * @return the repository URL (optional)
     */
    String repositoryUrl() default "";
    
    /**
     * Business capabilities that this application provides.
     * 
     * <p>List the key business capabilities/use cases exposed by this service.</p>
     * 
     * <p>Examples for onboarding service:</p>
     * <ul>
     *   <li>"Customer Identity Verification"</li>
     *   <li>"Document Management"</li>
     *   <li>"Account Creation"</li>
     * </ul>
     * 
     * @return array of business capabilities (optional)
     */
    String[] capabilities() default {};
    
    /**
     * Whether this application is deprecated.
     * 
     * <p>Deprecated applications should have a migration plan and sunset date.</p>
     * 
     * @return true if deprecated, false otherwise (default: false)
     */
    boolean deprecated() default false;
    
    /**
     * Deprecation message explaining why and what to use instead.
     * 
     * <p>Example: "Replaced by customer-application-onboarding-v2. Migrate by Q2 2025."</p>
     * 
     * @return deprecation message (optional, only if deprecated=true)
     */
    String deprecationMessage() default "";
}
