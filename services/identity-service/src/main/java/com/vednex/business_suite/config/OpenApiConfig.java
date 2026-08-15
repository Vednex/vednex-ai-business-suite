package com.vednex.business_suite.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "Vednex AI Business Suite API",
				version = "0.1.0",
				description = "Milestone 1 API surface for the Vednex AI Business Suite modular monolith.",
				contact = @Contact(name = "Vednex")
		),
		servers = @Server(url = "/", description = "Current environment")
)
public class OpenApiConfig {
}
