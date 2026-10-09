package com.weatherwise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI weatherWiseOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Roamora Journey Intelligence API")
                                .version("1.0.0")
                                .description(
                                        "API for weather-based route planning, " +
                                        "location search, weather information, " +
                                        "risk analysis and route history."
                                )
                                .contact(
                                        new Contact()
                                                .name("Roamora Team")
                                )
                                .license(
                                        new License()
                                                .name("Apache 2.0")
                                                .url(
                                                        "https://www.apache.org/licenses/LICENSE-2.0"
                                                )
                                )
                );
    }
}