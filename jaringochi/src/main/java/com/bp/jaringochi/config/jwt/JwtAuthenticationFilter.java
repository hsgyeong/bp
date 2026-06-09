package com.bp.jaringochi.config.jwt;


import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.bp.jaringochi.domain.user.dto.User;
import com.bp.jaringochi.domain.user.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {	// 모든 요청에서 JWT를 검사하는 필터 클래스
	
	private final JwtUtil jwtUtil;
	private final UserService userService;
	
	@Override	
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) // 실제 필터 로직이 실행되는 메서드
			throws ServletException, IOException {
		
		String authorization = request.getHeader("Authorization");			// 요청 헤더에서 Authorization 값을 꺼냄
		
		if (authorization != null && authorization.startsWith("Bearer ")) {  // Authorization 헤더가 있고 Bearer 토큰 형식인지 확인
			String token = authorization.substring(7);	// "Bearer " 7글자를 제거하고 실제 JWT만 추출
			
			try { 
				if (jwtUtil.validateToken(token)) {			// 유효한 정상 토큰이라면
					Long userId = jwtUtil.getUserId(token);	// JWT에서 userId를 꺼냄
					User user = userService.findById(userId); // userId로 DB에서 사용자 정보 가져옴
				
					UsernamePasswordAuthenticationToken authentication = 
							new UsernamePasswordAuthenticationToken(
									user,		// 인증된 사용자 정보; 컨트롤러에서 Authentication.getPrincipal()로 꺼낼 수 있음
									null,		// 비밀번호 자리; 이미 인증된 요청이므로 null
									List.of()	// 권한 목록; 아직 ROLE_USER같은 권한을 안 써서 빈 리스트
							);
				
					SecurityContextHolder.getContext().setAuthentication(authentication);	// 요청을 인증된 요청으로 등록
				}
			} catch (Exception e) {
				// 토큰이 잘못됐거나 사용자 조회 실패시 인증 정보 비움
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);		
	}
}
	
