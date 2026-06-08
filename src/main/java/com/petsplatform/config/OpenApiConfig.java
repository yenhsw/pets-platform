package com.petsplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration.
 *
 * <p>Disables springdoc auto-configuration to avoid bean name conflicts,
 * and registers a single custom {@link OpenAPI} bean instead.
 *
 * <p>Requires: springdoc-openapi-starter-webmvc-ui
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Pets Platform API")
                .version("1.0.0")
                .description("""
                    REST API for the Pets Platform application.

                    ## Modules
                    - **TestApi** — Demo module showcasing all database patterns

                    ## Authentication
                    Currently open (no auth). Add Spring Security filter to protect endpoints.

                    ## Response Format
                    All responses follow the `ApiResponse<T>` wrapper:
                    ```json
                    {
                      "success": true,
                      "message": "Success",
                      "data": { ... },
                      "timestamp": "2026-06-08T10:00:00Z",
                      "error": null
                    }
                    ```
                    """)
                .contact(new Contact()
                    .name("Pets Platform Team")
                    .email("team@petsplatform.com"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local Development"),
                new Server()
                    .url("/")
                    .description("Production")));
    }
}
