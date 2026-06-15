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
	private final RefreshTokenService refreshTokenService;
	
	public LoginResponse login(LoginRequest request) {
		
		try {
			Authentication authentication = authenticationManager.authenticate(		// 사용자가 입력한 email/password를 Spring Security 인증 토큰으로 생성
					new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
			);
		
			User user = userDao.findByEmail(authentication.getName());
		
			if (user == null) {
				throw new BusinessException(ErrorCode.USER_NOT_FOUND);
			}
		
			String accessToken = jwtTokenProvider.generateToken(authentication, user.getId());
			String refreshToken = refreshTokenService.createRefreshToken(user.getId());

			String role = authentication.getAuthorities().stream()
			        .map(GrantedAuthority::getAuthority)
			        .filter(authority -> authority.startsWith("ROLE_"))
			        .findFirst()
			        .orElse("ROLE_USER");

			return new LoginResponse(
			        accessToken,
			        refreshToken,
			        "Bearer",
			        authentication.getName(),
			        role
			);
			
		} catch (AuthenticationException e) {
			throw new BusinessException(ErrorCode.INVALID_LOGIN);
		}
	}
}
