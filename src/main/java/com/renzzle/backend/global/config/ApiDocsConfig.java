package com.renzzle.backend.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnProperty(name = "docs.enabled", havingValue = "true")
public class ApiDocsConfig implements WebMvcConfigurer {

    // Serves the Scalar API reference page (templates/docs.html) at /docs.
    // A view controller renders the template in place, so unlike a forward it does
    // not re-enter the security filter chain under a second path.
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/docs").setViewName("docs");
    }

}
