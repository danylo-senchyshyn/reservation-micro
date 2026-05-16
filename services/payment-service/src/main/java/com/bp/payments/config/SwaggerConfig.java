package com.bp.payments.config;

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
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server().url("http://localhost:8080").description("Gateway")))
                .info(new Info()
                        .title("Payment Service API")
                        .description("Manages payments. Payment is auto-created (PENDING) when a reservation is made via Kafka event. Use /confirm or /fail to manually trigger the flow.")
                        .version("1.0.0"));
    }

    @Bean
    public OpenApiCustomizer paymentExamplesCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            if (path.equals("/api/payments/{id}/fail") && pathItem.getPost() != null) {
                pathItem.getPost().getRequestBody()
                        .setContent(new Content().addMediaType("application/json",
                                new MediaType().addExamples("default",
                                        new Example()
                                                .summary("Fail payment with reason")
                                                .value("{\"reason\": \"insufficient_funds\"}"))));
            }
        });
    }
}
