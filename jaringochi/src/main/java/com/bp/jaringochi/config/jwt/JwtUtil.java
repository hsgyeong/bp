package com.bp.jaringochi.config.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {	// JWT 생성, 검증, 사용자 ID 추출 등을 담당하는 유틸 클래스
	
	private final SecretKey key;
	private final long expirationMillis;	// JWT가 몇 밀리초 동안 유효한지 저장
	
	public JwtUtil(
		@Value("${jwt.secret}") String secret, 					// application.properties의 jwt.secret 값을 가져옴
		@Value("${jwt.expiration}") long expirationMillis) {	// application.properties의 jwt.expiration 값을 가져옴
			this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // 문자열 secret을 JWT 서명용 SecretKey로 변환
			this.expirationMillis = expirationMillis;
	}
	
	public String createToken(Long userId, String email) {				// 로그인 성공 시 JWT 문자열을 만드는 메소드
		Date now = new Date();											
		Date expiredAt = new Date(now.getTime() + expirationMillis); 	// 현재 시간 + 유효 시간 = 토큰 만료 시간
		
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("email", email)	// 토큰 안에 email 을 추가 정보로 저장
				.issuedAt(now)			// 토큰 발급 시간
				.expiration(expiredAt)	// 토큰 만료 시간
				.signWith(key)			// 비밀키로 토큰에 서명
				.compact();				// JWT를 최종 문자열 형태로 변환해서 반환
	}
	
	public Long getUserId(String token) {
		return Long.valueOf(getClaims(token).getSubject());
	}
	
	private Claims getClaims(String token) {
		return Jwts.parser()
				.verifyWith(key)			// 비밀키로 서명 검증 설정
				.build()					// 파서 생성 완료
				.parseSignedClaims(token)	// JWT 문자열 파싱하고 서명 검증
				.getPayload();				// JWT 안에 있는 claims를 반환
	}
	
	public boolean validateToken(String token) {
		try {
			getClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
}
