package com.bp.jaringochi.domain.user.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import com.bp.jaringochi.domain.user.dao.UserDao;
import com.bp.jaringochi.domain.user.dto.LoginRequest;
import com.bp.jaringochi.domain.user.dto.LoginResponse;
import com.bp.jaringochi.domain.user.dto.User;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.bp.jaringochi.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

/**
 * 인증 비즈니스 로직.
 * 실제 비밀번호 비교는 직접 하지 않고 AuthenticationManager 에 위임한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {
	
	private final AuthenticationManager authenticationManager;	 // Spring Security 인증 관리자
	private final JwtTokenProvider jwtTokenProvider;			 // JWT Access Token 발급 전용 컴포넌트
	private final UserDao userDao;
	
	public LoginResponse login(LoginRequest request) {
		
		try {
			Authentication authentication = authenticationManager.authenticate(		// 사용자가 입력한 email/password를 Spring Security 인증 토큰으로 생성
					new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
			);
		
			User user = userDao.findByEmail(authentication.getName());
		
			if (user == null) {
				throw new BusinessException(ErrorCode.USER_NOT_FOUND);
			}
		
			String token = jwtTokenProvider.generateToken(authentication, user.getId());	// userId를 subject로 넣은 JWT를 발급
		
			String role = authentication.getAuthorities().stream()		// 현재 로그인한 사용자의 권한 목록을 가져옴
					.map(GrantedAuthority::getAuthority)				// 객체에서 실제 권한 문자열만 꺼냄
					.filter(authority -> authority.startsWith("ROLE_"))
					.findFirst().orElse("ROLE_USER");					 // findFirst() 결과가 있으면 그 값을 쓰고, 없으면 기본값으로 "ROLE_USER"를 사용

			return new LoginResponse(token, "Bearer", authentication.getName(), role);		// 클라이언트에 Access Token과 기본 로그인 정보를 반환
			
		} catch (AuthenticationException e) {
			throw new BusinessException(ErrorCode.INVALID_LOGIN);
		}
	}
}
