package org.fireflyframework.common.application.spi;

import java.util.Map;

/**
 * SPI interface for provider/integration configuration.
 * <p>
 * Represents the configuration of an external provider (e.g., payment processor,
 * notification service, identity provider). Platform implementations should provide
 * concrete implementations with domain-specific configuration properties.
 * </p>
 */
public interface ProviderConfiguration {

    /**
     * @return the type/identifier of this provider
     */
    String getProviderType();

    /**
     * @return the display name of this provider
     */
    String getName();

    /**
     * @return whether this provider is currently enabled
     */
    boolean isEnabled();

    /**
     * @return additional configuration properties as key-value pairs
     */
    Map<String, Object> getProperties();
}
