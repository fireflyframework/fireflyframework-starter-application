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

package org.fireflyframework.common.application.aop;

import org.fireflyframework.common.application.config.ApplicationLayerProperties;
import org.fireflyframework.common.application.context.AppContext;
import org.fireflyframework.common.application.context.AppSecurityContext;
import org.fireflyframework.common.application.context.ApplicationExecutionContext;
import org.fireflyframework.common.application.security.EndpointSecurityRegistry;
import org.fireflyframework.common.application.security.SecurityAuthorizationService;
import org.fireflyframework.common.application.security.annotation.Secure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
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
@RequiredArgsConstructor
@Slf4j
public class SecurityAspect {
    
    private final SecurityAuthorizationService authorizationService;
    private final EndpointSecurityRegistry endpointSecurityRegistry;
    private final ApplicationLayerProperties properties;
    
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
        
        // Check if security is disabled
        if (!properties.getSecurity().isEnabled()) {
            log.debug("Security is disabled, allowing access to {}", endpoint);
            return joinPoint.proceed();
        }

        // Build the reactive authorization chain
        Mono<?> authorizationChain = authorizationService.authorize(executionContext.getContext(), securityContext)
                .flatMap(authorizedContext -> {
                    if (!authorizedContext.isAuthorized()) {
                        if (!properties.getSecurity().isEnforce()) {
                            log.warn("ACCESS WOULD BE DENIED (enforce=false) for party: {} to endpoint: {}, reason: {}",
                                    executionContext.getPartyId(),
                                    securityContext.getEndpoint(),
                                    authorizedContext.getAuthorizationFailureReason());
                        } else {
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
                    }

                    try {
                        Object result = joinPoint.proceed();
                        if (result instanceof Mono) {
                            return (Mono<?>) result;
                        }
                        return Mono.justOrEmpty(result);
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                });

        // Determine return type: if the intercepted method returns Mono, return the chain directly.
        // Otherwise, block on a bounded-elastic scheduler to avoid deadlocking Netty event loops.
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        if (Mono.class.isAssignableFrom(sig.getReturnType())) {
            return authorizationChain;
        }
        return authorizationChain
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .block();
    }
    
    /**
     * Intercepts classes annotated with @Secure.
     * 
     * @param joinPoint the join point
     * @param secure the secure annotation
     * @return the method result
     * @throws Throwable if method execution fails
     */
    @Around("@within(secure) && !@annotation(org.fireflyframework.common.application.security.annotation.Secure)")
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

        // Use SECURITY_CENTER only if both the global property and annotation agree
        boolean useSecurityCenter = properties.getSecurity().isUseSecurityCenter() && secure.useSecurityCenter();
        AppSecurityContext.SecurityConfigSource configSource = useSecurityCenter
                ? AppSecurityContext.SecurityConfigSource.SECURITY_CENTER
                : AppSecurityContext.SecurityConfigSource.ANNOTATION;

        return AppSecurityContext.builder()
                .endpoint(endpoint)
                .httpMethod(httpMethod)
                .requiredRoles(roles)
                .requiredPermissions(permissions)
                .requiresAuthentication(secure.requiresAuthentication())
                .allowAnonymous(secure.allowAnonymous())
                .configSource(configSource)
                .build();
    }
    
    /**
     * Extracts endpoint path from join point by reading Spring MVC mapping annotations.
     * Falls back to class.method signature if no mapping annotations found.
     */
    private String extractEndpoint(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> declaringClass = method.getDeclaringClass();

        // Get class-level base path
        String basePath = "";
        RequestMapping classMapping = declaringClass.getAnnotation(RequestMapping.class);
        if (classMapping != null && classMapping.value().length > 0) {
            basePath = classMapping.value()[0];
        }

        // Get method-level path from various mapping annotations
        String methodPath = extractMethodPath(method);

        if (methodPath != null) {
            return normalizePath(basePath + methodPath);
        }

        // Fallback to class.method signature
        return signature.getDeclaringTypeName() + "." + signature.getName();
    }

    private String extractMethodPath(Method method) {
        GetMapping get = method.getAnnotation(GetMapping.class);
        if (get != null) return get.value().length > 0 ? get.value()[0] : "";

        PostMapping post = method.getAnnotation(PostMapping.class);
        if (post != null) return post.value().length > 0 ? post.value()[0] : "";

        PutMapping put = method.getAnnotation(PutMapping.class);
        if (put != null) return put.value().length > 0 ? put.value()[0] : "";

        DeleteMapping delete = method.getAnnotation(DeleteMapping.class);
        if (delete != null) return delete.value().length > 0 ? delete.value()[0] : "";

        PatchMapping patch = method.getAnnotation(PatchMapping.class);
        if (patch != null) return patch.value().length > 0 ? patch.value()[0] : "";

        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        if (mapping != null) return mapping.value().length > 0 ? mapping.value()[0] : "";

        return null;
    }

    /**
     * Extracts HTTP method from join point by reading Spring MVC mapping annotations.
     * Falls back to "UNKNOWN" if no mapping annotation found.
     */
    private String extractHttpMethod(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";

        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        if (mapping != null && mapping.method().length > 0) {
            return mapping.method()[0].name();
        }

        return "UNKNOWN";
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "/";
        if (!path.startsWith("/")) path = "/" + path;
        return path.replaceAll("//+", "/");
    }
}
