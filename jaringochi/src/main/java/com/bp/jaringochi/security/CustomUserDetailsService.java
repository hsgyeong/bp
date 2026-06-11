package com.bp.jaringochi.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bp.jaringochi.domain.user.dao.UserDao;
import com.bp.jaringochi.domain.user.dto.User;

import lombok.RequiredArgsConstructor;
/**
 * '사용자가 누구인가'(UserDetails) 를 반환하는 Spring Security 표준 인터페이스 구현체.
 * 로그인 시 AuthenticationManager 가 이 서비스를 호출해 사용자를 찾고,
 * 반환된 UserDetails 의 password(BCrypt 해시) 와 입력 비밀번호를 내부적으로 비교한다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
	
	private final UserDao userDao;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		
		User user = userDao.findByEmail(email);
		if (user == null) {
			throw new UsernameNotFoundException("사용자를 찾을 수 없습니다. " + email);
		}
		
		// 프로젝트의 User DTO를 Spring Security의 UserDetails로 변환
		return org.springframework.security.core.userdetails.User.builder()
				.username(user.getEmail())
				.password(user.getPassword())
				.roles("USER")
				.build();
	}
	
	
}
