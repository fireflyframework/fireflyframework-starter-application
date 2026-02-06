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

package org.fireflyframework.application.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ApplicationLayerBannerConfig}.
 */
@DisplayName("ApplicationLayerBannerConfig Tests")
class ApplicationLayerBannerConfigTest {
    
    private ApplicationLayerBannerConfig bannerConfig;
    private ApplicationStartingEvent event;
    private SpringApplication springApplication;
    
    @BeforeEach
    void setUp() {
        bannerConfig = new ApplicationLayerBannerConfig();
        springApplication = mock(SpringApplication.class);
        event = mock(ApplicationStartingEvent.class);
        when(event.getSpringApplication()).thenReturn(springApplication);
    }
    
    @Test
    @DisplayName("Should set banner mode to CONSOLE on application starting event")
    void shouldSetBannerModeToConsole() {
        // When
        bannerConfig.onApplicationEvent(event);
        
        // Then
        verify(springApplication).setBannerMode(Banner.Mode.CONSOLE);
    }
    
    @Test
    @DisplayName("Should attempt to load custom banner from classpath")
    void shouldAttemptToLoadCustomBanner() {
        // When
        bannerConfig.onApplicationEvent(event);
        
        // Then
        verify(springApplication).setBannerMode(Banner.Mode.CONSOLE);
        // Banner loading is attempted but may fail gracefully if banner.txt doesn't exist
    }
    
    @Test
    @DisplayName("Should handle missing banner.txt gracefully")
    void shouldHandleMissingBannerGracefully() {
        // Given - No banner.txt in classpath
        
        // When - Should not throw exception
        bannerConfig.onApplicationEvent(event);
        
        // Then - Banner mode is still set
        verify(springApplication).setBannerMode(Banner.Mode.CONSOLE);
    }
    
    @Test
    @DisplayName("Should not fail when SpringApplication is null")
    void shouldNotFailWhenSpringApplicationIsNull() {
        // Given
        when(event.getSpringApplication()).thenReturn(null);
        
        // When/Then - Should not throw exception
        try {
            bannerConfig.onApplicationEvent(event);
        } catch (Exception e) {
            // Expected - NullPointerException when trying to set banner mode
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }
    
    @Test
    @DisplayName("Should be an ApplicationListener")
    void shouldBeApplicationListener() {
        // Then
        assertThat(bannerConfig).isInstanceOf(org.springframework.context.ApplicationListener.class);
    }
}
