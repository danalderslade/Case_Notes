package com.casemanagement.notes.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI caseNotesOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Case Notes API")
                .description("Case notes service with per-country table routing")
                .version("1.0.0")
                .contact(new Contact().name("Case Management Team")));
    }
}
