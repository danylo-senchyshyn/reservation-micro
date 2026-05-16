package com.bp.users.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("http://localhost:8080").description("Gateway")))
                .info(new Info()
                        .title("User Service API")
                        .description("Manages user accounts. Start here — create a user first, then use the returned ID for reservations.")
                        .version("1.0.0"));
    }

    @Bean
    public OpenApiCustomizer userExamplesCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            if (path.equals("/api/users") && pathItem.getPost() != null) {
                pathItem.getPost().getRequestBody()
                        .setContent(new Content().addMediaType("application/json",
                                new MediaType().addExamples("default",
                                        new Example()
                                                .summary("Demo user for thesis defense")
                                                .value("{\"email\": \"jan.novak@example.com\", \"fullName\": \"Jan Novak\"}"))));
            }
        });
    }
}
