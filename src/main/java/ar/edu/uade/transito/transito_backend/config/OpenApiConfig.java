package ar.edu.uade.transito.transito_backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI trafficOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Tránsito Municipal")
                        .version("1.0.0")
                        .description("API REST del módulo de Tránsito, Estacionamiento y Seguridad Vial."))
                .servers(List.of(new Server().url("http://localhost:8080").description("Entorno local")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
