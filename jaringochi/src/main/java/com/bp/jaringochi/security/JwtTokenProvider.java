package com.bp.jaringochi.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;


import lombok.RequiredArgsConstructor;

/**
 * JWT 발급(issue) 전용 컴포넌트
 * Spring Security가 제공하는 JwtEncoder(NimbusJwtEncoder)를 주입받아 토큰을 발급한다.
 * 토큰 검증(decode/validate)은 더 이상 작성하지 않는다. (Resource server가 자동 처리)
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
	
    // SecurityConfig에서 등록한 NimbusJwtEncoder가 주입된다.
    // 이 encoder가 secret key로 JWT에 서명한다.
	private final JwtEncoder encoder;
	
	@Value("${jwt.expiration}")
	private long expirationMs;
	
	
	// 인증된 사용자(Authentication)로부터 Access Token을 발급한다.
	public String generateToken(Authentication authentication, Long userId) {
		Instant now = Instant.now();
		
		String role = authentication.getAuthorities().stream()		   // 현재 로그인한 사용자의 권한 목록을 가져옴
				.map(GrantedAuthority::getAuthority)				   // 객체에서 실제 권한 문자열만 꺼냄
				.filter(authority -> authority.startsWith("ROLE_"))
				.findFirst().orElse("ROLE_USER");					   // findFirst() 결과가 있으면 그 값을 쓰고, 없으면 기본값으로 "ROLE_USER"를 사용
		
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("jaringochi")									// 토큰 발급자
				.issuedAt(now)											// 토큰 발급 시각
				.expiresAt(now.plus(expirationMs, ChronoUnit.MILLIS))   // 토큰 만료 시각
				.subject(String.valueOf(userId))						// 토큰의 주 식별자
				.claim("email", authentication.getName())				// 부가 정보: 로그인 email
				.claim("role", role)									// 부가 정보: 권한
				.build();
		
		//  // JWT 서명 알고리즘. HS256으로 서명
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
	 
}
