package com.bp.reservations.config;

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
public class SwaggerConfig {

    @Bean
    public OpenAPI reservationServiceOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("http://localhost:8080").description("Gateway")))
                .info(new Info()
                        .title("Reservation Service API")
                        .description("Manages reservations. Step 2: create a reservation using userId from User Service. After creation, Outbox publishes event → Payment Service auto-creates a PENDING payment within ~5 seconds.")
                        .version("1.0.0"));
    }

    @Bean
    public OpenApiCustomizer reservationExamplesCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            if (path.equals("/api/reservations") && pathItem.getPost() != null) {
                pathItem.getPost().getRequestBody()
                        .setContent(new Content().addMediaType("application/json",
                                new MediaType().addExamples("default",
                                        new Example()
                                                .summary("Demo reservation (replace userId with real ID)")
                                                .value("{\"userId\": 1, \"resourceId\": 5, \"from\": \"2027-06-01T10:00:00\", \"to\": \"2027-06-01T12:00:00\"}"))));
            }
        });
    }
}
