package com.renzzle.backend.global.config;

import com.renzzle.backend.domain.auth.domain.GrantType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "Authorization";

        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.addList(jwtSchemeName);

        SecurityScheme securityScheme = new SecurityScheme();
        securityScheme.name(jwtSchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme(GrantType.BEARER.getType());

        Components components = new Components();
        components.addSecuritySchemes(jwtSchemeName, securityScheme);

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components)
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Renzzle Documents")
                .description("Renzzle Swagger UI")
                .version("1.0.0");
    }

}
