package com.aritzia.availability.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permissive CORS on this read-only, unauthenticated GET endpoint so it can be
 * called directly from a browser-based demo page. No session/cookie data is
 * involved, so allowing any origin carries no meaningful risk here.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/availability/**")
                .allowedOrigins("*")
                .allowedMethods("GET");
    }
}
