package org.fireflyframework.common.application.spi;

import java.util.List;
import java.util.Map;

/**
 * SPI interface representing a user session context.
 * <p>
 * Platform-specific implementations should provide concrete implementations
 * that carry session metadata, user roles, scopes, and any domain-specific context.
 * </p>
 */
public interface SessionContext {

    /**
     * @return the unique identifier of the authenticated user
     */
    String getUserId();

    /**
     * @return the tenant/organization identifier
     */
    String getTenantId();

    /**
     * @return the user's roles in the current session
     */
    List<String> getRoles();

    /**
     * @return the user's permissions/scopes in the current session
     */
    List<String> getScopes();

    /**
     * @return additional session attributes as key-value pairs
     */
    Map<String, Object> getAttributes();
}
