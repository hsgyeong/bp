package com.bp.jaringochi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	/*
	  PasswordEncoder Bean 분리
	  
	  SecurityConfig에서 PasswordEncoder를 직접 선언시
	  인증 관련 Bean들과 의존성이 얽혀 순환 참조 발생
	  따라서 비밀번호 암호화 설정만 별도 Config로 분리
	 */
}
