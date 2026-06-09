package com.bp.jaringochi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.bp.jaringochi.config.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	
	@Bean   // 보안 규칙을 설정하는 메소드
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {	
		
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)	// JWT 방식이므로 서버 세션을 만들지 않음
		)
		
		.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/signup", "/api/auth/login", "/swagger-ui/**", "/v3/api-docs/**").permitAll()  // 회원가입과 로그인은 토큰 없이 접근 허용
				.anyRequest().authenticated() // 그 외 모든 요청은 로그인, 즉 JWT 인증 필요
		
		)
		
		.addFilterBefore(		// JWT 필터를 Spring Security 필터 체인에 추가
				jwtAuthenticationFilter, 
				UsernamePasswordAuthenticationFilter.class  // 이 필터보다 먼저 JWT 필터가 실행되게 함
		);
		
		return http.build();
			
	}
	
}
