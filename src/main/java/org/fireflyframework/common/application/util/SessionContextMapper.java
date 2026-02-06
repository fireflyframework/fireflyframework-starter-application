package org.fireflyframework.application.util;

import org.fireflyframework.common.application.spi.SessionContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for mapping SessionContext to roles and permissions.
 * <p>
 * This mapper extracts roles and permissions from the generic {@link SessionContext} SPI.
 * Platform-specific implementations should provide a {@link SessionContext} that
 * populates roles, scopes, and attributes according to their domain model.
 * </p>
 *
 * @author Firefly Framework Team
 * @since 1.0.0
 */
@Slf4j
public final class SessionContextMapper {

    private SessionContextMapper() {
        // Utility class
    }

    /**
     * Extracts roles from the session context.
     *
     * @param sessionContext the session context
     * @param scopeKey optional scope key for filtering (e.g., contractId)
     * @param subScopeKey optional sub-scope key for filtering (e.g., productId)
     * @return set of role strings
     */
    public static Set<String> extractRoles(SessionContext sessionContext, UUID scopeKey, UUID subScopeKey) {
        if (sessionContext == null) {
            log.debug("Session context is null, returning empty roles");
            return Collections.emptySet();
        }

        List<String> roles = sessionContext.getRoles();
        if (roles == null || roles.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>(roles);
        log.debug("Extracted {} roles from session context", result.size());
        return result;
    }

    /**
     * Extracts permissions/scopes from the session context.
     *
     * @param sessionContext the session context
     * @param scopeKey optional scope key for filtering
     * @param subScopeKey optional sub-scope key for filtering
     * @return set of permission strings
     */
    public static Set<String> extractPermissions(SessionContext sessionContext, UUID scopeKey, UUID subScopeKey) {
        if (sessionContext == null) {
            log.debug("Session context is null, returning empty permissions");
            return Collections.emptySet();
        }

        List<String> scopes = sessionContext.getScopes();
        if (scopes == null || scopes.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>(scopes);
        log.debug("Extracted {} permissions from session context", result.size());
        return result;
    }

    /**
     * Checks if the session has a specific attribute.
     *
     * @param sessionContext the session context
     * @param attributeKey the attribute key to check
     * @return true if the attribute exists
     */
    public static boolean hasAttribute(SessionContext sessionContext, String attributeKey) {
        if (sessionContext == null || sessionContext.getAttributes() == null) {
            return false;
        }
        return sessionContext.getAttributes().containsKey(attributeKey);
    }

    /**
     * Retrieves an attribute value from the session context.
     *
     * @param sessionContext the session context
     * @param attributeKey the attribute key
     * @param type the expected type
     * @return the attribute value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAttribute(SessionContext sessionContext, String attributeKey, Class<T> type) {
        if (sessionContext == null || sessionContext.getAttributes() == null) {
            return null;
        }
        Object value = sessionContext.getAttributes().get(attributeKey);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
}
