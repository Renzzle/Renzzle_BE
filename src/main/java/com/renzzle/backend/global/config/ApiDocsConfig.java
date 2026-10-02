package com.renzzle.backend.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(name = "docs.enabled", havingValue = "true")
public class ApiDocsConfig implements WebMvcConfigurer {

    // Renders templates/docs.html in place, so it doesn't re-enter the security chain
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/docs").setViewName("docs");
    }

}
