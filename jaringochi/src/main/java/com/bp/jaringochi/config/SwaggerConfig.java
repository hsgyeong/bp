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
		
		String securitySchemeName = "bearerAuth";	// security scheme과 requirement 연결하는 키
		
		return new OpenAPI()
				.info(new Info()					// API 문서 기본 정보
						.title("Jaringochi REST API")
						.version("1.0")
						.description("Jaringochi API 문서"))
				
				.components(new Components()		// Swagger에 Authorization: Bearer <JWT> 방식의 인증을 사용한다고 등록 (Swagger UI에 Authorize 버튼 표시)
						.addSecuritySchemes(securitySchemeName, 
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")))
				
				.addSecurityItem(new SecurityRequirement()	// bearerAuth 인증 방식을 API 요청에 적용
						.addList(securitySchemeName));		// Authorize에 입력한 토큰이 Swagger 요청의 Authorization 헤더에 자동으로 붙는다.
	}
}
