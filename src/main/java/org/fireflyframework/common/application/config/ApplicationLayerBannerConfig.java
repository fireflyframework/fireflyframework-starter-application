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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.ResourceBanner;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Configuration class for Application Layer banner.
 * Ensures the Firefly Application Layer banner is displayed on startup.
 * 
 * <p>The banner identifies that this microservice is built using
 * lib-common-application and belongs to the Application Layer.</p>
 * 
 * @author Firefly Development Team
 * @since 1.0.0
 */
@Slf4j
public class ApplicationLayerBannerConfig implements ApplicationListener<ApplicationStartingEvent> {
    
    @Override
    public void onApplicationEvent(ApplicationStartingEvent event) {
        try {
            // Set banner mode
            event.getSpringApplication().setBannerMode(Banner.Mode.CONSOLE);
            
            // Try to load custom banner
            Resource bannerResource = new ClassPathResource("banner.txt");
            if (bannerResource.exists()) {
                event.getSpringApplication().setBanner(new ResourceBanner(bannerResource));
                log.debug("Loaded Application Layer banner from classpath");
            }
        } catch (Exception e) {
            log.debug("Could not load custom banner, using default", e);
        }
    }
}
