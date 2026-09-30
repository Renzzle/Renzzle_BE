package com.renzzle.backend.global.config;

import com.renzzle.backend.domain.auth.domain.GrantType;
import com.renzzle.backend.global.security.AppKeyAuthenticationFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "Authorization";
        String appKeySchemeName = AppKeyAuthenticationFilter.APP_KEY_HEADER;

        SecurityRequirement securityRequirement = new SecurityRequirement();
        securityRequirement.addList(jwtSchemeName);
        securityRequirement.addList(appKeySchemeName);

        SecurityScheme securityScheme = new SecurityScheme();
        securityScheme.name(jwtSchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme(GrantType.BEARER.getType());

        // Every /api request is rejected without this header
        SecurityScheme appKeyScheme = new SecurityScheme();
        appKeyScheme.name(appKeySchemeName)
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER);

        Components components = new Components();
        components.addSecuritySchemes(jwtSchemeName, securityScheme);
        components.addSecuritySchemes(appKeySchemeName, appKeyScheme);

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components)
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Renzzle Documents")
                .description("Renzzle API Documents")
                .version("1.0.0");
    }

}
