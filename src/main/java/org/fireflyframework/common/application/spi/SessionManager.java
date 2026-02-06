package org.fireflyframework.common.application.spi;

import reactor.core.publisher.Mono;

/**
 * SPI interface for session management.
 * <p>
 * Implementations of this interface provide the mechanism for retrieving and managing
 * user session contexts. Platform-specific implementations (e.g., for banking, e-commerce)
 * should implement this interface to integrate with their identity/session infrastructure.
 * </p>
 */
public interface SessionManager<T> {

    /**
     * Retrieves the current session context for the given token.
     *
     * @param token the authentication token
     * @return a Mono emitting the session context
     */
    Mono<T> getSessionContext(String token);

    /**
     * Validates whether the given token represents a valid session.
     *
     * @param token the authentication token
     * @return a Mono emitting true if the session is valid
     */
    Mono<Boolean> isSessionValid(String token);

    /**
     * Invalidates the session associated with the given token.
     *
     * @param token the authentication token
     * @return a Mono completing when the session is invalidated
     */
    Mono<Void> invalidateSession(String token);
}
