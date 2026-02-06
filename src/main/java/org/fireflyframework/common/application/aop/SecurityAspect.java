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

package org.fireflyframework.application.aop;

import org.fireflyframework.application.context.AppContext;
import org.fireflyframework.application.context.AppSecurityContext;
import org.fireflyframework.application.context.ApplicationExecutionContext;
import org.fireflyframework.application.security.EndpointSecurityRegistry;
import org.fireflyframework.application.security.SecurityAuthorizationService;
import org.fireflyframework.application.security.annotation.Secure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Aspect for intercepting and processing @Secure annotations.
 * Handles security checks before method execution.
 * 
 * <p>This aspect intercepts methods annotated with @Secure and performs
 * authorization checks using the SecurityAuthorizationService.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAspect {
    
    private final SecurityAuthorizationService authorizationService;
    private final EndpointSecurityRegistry endpointSecurityRegistry;
    
    /**
     * Intercepts methods annotated with @Secure.
     * 
     * @param joinPoint the join point
     * @param secure the secure annotation
     * @return the method result
     * @throws Throwable if method execution fails
     */
    @Around("@annotation(secure)")
    public Object secureMethod(ProceedingJoinPoint joinPoint, Secure secure) throws Throwable {
        log.debug("Intercepting @Secure method: {}", joinPoint.getSignature().getName());
        
        // Extract ApplicationExecutionContext from method arguments
        ApplicationExecutionContext executionContext = findExecutionContext(joinPoint.getArgs());
        if (executionContext == null) {
            log.warn("No ApplicationExecutionContext found in method arguments, skipping security check");
            return joinPoint.proceed();
        }
        
        // Check EndpointSecurityRegistry first (explicit configuration overrides annotations)
        String endpoint = extractEndpoint(joinPoint);
        String httpMethod = extractHttpMethod(joinPoint);
        
        AppSecurityContext securityContext = endpointSecurityRegistry
                .getEndpointSecurity(endpoint, httpMethod)
                .map(explicitSecurity -> {
                    log.debug("Using EXPLICIT security configuration for {} {}", httpMethod, endpoint);
                    return explicitSecurity.toSecurityContext(endpoint, httpMethod);
                })
                .orElseGet(() -> {
                    log.debug("Using ANNOTATION security configuration for {} {}", httpMethod, endpoint);
                    return buildSecurityContext(secure, joinPoint, endpoint, httpMethod);
                });
        
        // Perform authorization
        return authorizationService.authorize(executionContext.getContext(), securityContext)
                .flatMap(authorizedContext -> {
                    if (!authorizedContext.isAuthorized()) {
                        log.warn("Access denied for party: {} to endpoint: {}, reason: {}",
                                executionContext.getPartyId(),
                                securityContext.getEndpoint(),
                                authorizedContext.getAuthorizationFailureReason());
                        return Mono.error(new AccessDeniedException(
                                authorizedContext.getAuthorizationFailureReason() != null
                                        ? authorizedContext.getAuthorizationFailureReason()
                                        : "Access denied"
                        ));
                    }
                    
                    try {
                        Object result = joinPoint.proceed();
                        if (result instanceof Mono) {
                            return (Mono<?>) result;
                        }
                        return Mono.just(result);
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                })
                .block(); // Note: In reactive applications, you might want to handle this differently
    }
    
    /**
     * Intercepts classes annotated with @Secure.
     * 
     * @param joinPoint the join point
     * @param secure the secure annotation
     * @return the method result
     * @throws Throwable if method execution fails
     */
    @Around("@within(secure) && !@annotation(org.fireflyframework.application.security.annotation.Secure)")
    public Object secureClass(ProceedingJoinPoint joinPoint, Secure secure) throws Throwable {
        return secureMethod(joinPoint, secure);
    }
    
    /**
     * Finds ApplicationExecutionContext in method arguments.
     * 
     * @param args the method arguments
     * @return the execution context or null if not found
     */
    private ApplicationExecutionContext findExecutionContext(Object[] args) {
        if (args == null) {
            return null;
        }
        
        for (Object arg : args) {
            if (arg instanceof ApplicationExecutionContext) {
                return (ApplicationExecutionContext) arg;
            }
        }
        
        return null;
    }
    
    /**
     * Builds AppSecurityContext from @Secure annotation.
     * 
     * @param secure the annotation
     * @param joinPoint the join point
     * @param endpoint the endpoint path
     * @param httpMethod the HTTP method
     * @return the security context
     */
    private AppSecurityContext buildSecurityContext(Secure secure, ProceedingJoinPoint joinPoint, 
                                                    String endpoint, String httpMethod) {
        Set<String> roles = new HashSet<>(Arrays.asList(secure.roles()));
        Set<String> permissions = new HashSet<>(Arrays.asList(secure.permissions()));
        
        return AppSecurityContext.builder()
                .endpoint(endpoint)
                .httpMethod(httpMethod)
                .requiredRoles(roles)
                .requiredPermissions(permissions)
                .requiresAuthentication(secure.requiresAuthentication())
                .allowAnonymous(secure.allowAnonymous())
                .configSource(AppSecurityContext.SecurityConfigSource.ANNOTATION)
                .build();
    }
    
    /**
     * Extracts endpoint path from join point.
     * TODO: Extract from @RequestMapping annotations for accurate endpoint paths
     * 
     * @param joinPoint the join point
     * @return the endpoint path
     */
    private String extractEndpoint(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // TODO: Parse @RequestMapping, @GetMapping, @PostMapping etc. to get actual endpoint
        return signature.getDeclaringTypeName() + "." + signature.getName();
    }
    
    /**
     * Extracts HTTP method from join point.
     * TODO: Extract from @GetMapping, @PostMapping, @PutMapping, @DeleteMapping annotations
     * 
     * @param joinPoint the join point
     * @return the HTTP method
     */
    private String extractHttpMethod(ProceedingJoinPoint joinPoint) {
        // TODO: Parse method annotations to determine HTTP method
        return "UNKNOWN";
    }
}
