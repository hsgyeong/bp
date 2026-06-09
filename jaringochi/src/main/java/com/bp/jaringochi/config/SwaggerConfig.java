package com.bp.jaringochi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		
		String securitySchemeName = "bearerAuth";
		
		return new OpenAPI()
				.info(new Info()
						.title("Jaringochi REST API")
						.version("1.0")
						.description("Jaringochi API 문서"))
				.components(new Components()
						.addSecuritySchemes(securitySchemeName, 
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")))
				.addSecurityItem(new SecurityRequirement()
						.addList(securitySchemeName));
	}
}
