/*
 * Copyright 2025 Firefly Software Solutions Inc
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

package com.firefly.common.application.plugin.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Centralized ObjectMapper configuration for the plugin system.
 * 
 * <p>This component provides a properly configured ObjectMapper for plugin
 * input/output conversion with the following features:</p>
 * <ul>
 *   <li>Java 8 Date/Time support (JSR-310)</li>
 *   <li>Unknown properties ignored during deserialization</li>
 *   <li>Null values excluded from serialization</li>
 *   <li>Dates serialized as ISO-8601 strings (not timestamps)</li>
 * </ul>
 * 
 * <h3>Usage</h3>
 * <pre>
 * // Inject the component
 * &#64;Autowired
 * private PluginObjectMapperConfig objectMapperConfig;
 * 
 * // Use the ObjectMapper
 * MyType result = objectMapperConfig.getObjectMapper().convertValue(input, MyType.class);
 * 
 * // Or use static access for non-Spring contexts
 * ObjectMapper mapper = PluginObjectMapperConfig.getInstance();
 * </pre>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Component
public class PluginObjectMapperConfig {
    
    /**
     * Singleton instance for static access.
     */
    private static final AtomicReference<ObjectMapper> INSTANCE = new AtomicReference<>();
    
    /**
     * The configured ObjectMapper.
     */
    @Getter
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new PluginObjectMapperConfig with default settings.
     * Uses Spring's ObjectMapper if available, otherwise creates a new one.
     * 
     * @param springObjectMapper optional Spring-managed ObjectMapper
     */
    @Autowired(required = false)
    public PluginObjectMapperConfig(ObjectMapper springObjectMapper) {
        if (springObjectMapper != null) {
            // Use Spring's ObjectMapper but ensure it has our required modules
            this.objectMapper = configureMapper(springObjectMapper.copy());
        } else {
            this.objectMapper = createDefaultMapper();
        }
        
        // Set as singleton for static access
        INSTANCE.compareAndSet(null, this.objectMapper);
    }
    
    /**
     * Creates a PluginObjectMapperConfig with default settings (no Spring injection).
     */
    public PluginObjectMapperConfig() {
        this.objectMapper = createDefaultMapper();
        INSTANCE.compareAndSet(null, this.objectMapper);
    }
    
    /**
     * Gets the singleton ObjectMapper instance.
     * 
     * <p>This method provides static access to the ObjectMapper for contexts
     * where Spring injection is not available (e.g., static utility methods).</p>
     * 
     * <p><b>Note:</b> Prefer injecting PluginObjectMapperConfig when possible.</p>
     * 
     * @return the ObjectMapper instance
     */
    public static ObjectMapper getInstance() {
        ObjectMapper mapper = INSTANCE.get();
        if (mapper == null) {
            // Initialize with defaults if not yet created by Spring
            mapper = createDefaultMapper();
            if (!INSTANCE.compareAndSet(null, mapper)) {
                // Another thread beat us, use their instance
                mapper = INSTANCE.get();
            }
        }
        return mapper;
    }
    
    /**
     * Converts a value to the specified type.
     * 
     * @param value the value to convert
     * @param targetType the target type class
     * @param <T> the target type
     * @return the converted value
     * @throws IllegalArgumentException if conversion fails
     */
    public <T> T convert(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        }
        try {
            return objectMapper.convertValue(value, targetType);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to convert value to " + targetType.getName() + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates a default ObjectMapper with standard configuration.
     * 
     * @return a configured ObjectMapper
     */
    private static ObjectMapper createDefaultMapper() {
        return configureMapper(new ObjectMapper());
    }
    
    /**
     * Configures an ObjectMapper with standard settings.
     * 
     * @param mapper the mapper to configure
     * @return the configured mapper
     */
    private static ObjectMapper configureMapper(ObjectMapper mapper) {
        // Register Java 8 Date/Time module
        mapper.registerModule(new JavaTimeModule());
        
        // Serialization settings
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // Deserialization settings
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        
        return mapper;
    }
}
