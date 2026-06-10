package com.bp.jaringochi.domain.user.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.config.jwt.JwtUtil;
import com.bp.jaringochi.domain.user.dto.User;
import com.bp.jaringochi.domain.user.service.UserService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.bp.jaringochi.global.response.Response;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
	
	private final JwtUtil jwtUtil;	
	private final UserService userService;

	// 회원가입
    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Response<User> signup(@RequestBody User user) {
    	User created = userService.signup(user);
    	return Response.success("회원가입이 완료되었습니다.", created);
    }
    
    // 로그인
    @PostMapping("/auth/login")
    public Response<Map<String, Object>> login(@RequestBody User user) {
    	
    	if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }
    	
    	User loginUser = userService.login(user.getEmail(), user.getPassword());
    	
    	// JWT payload에 사용자 id와 email을 담아 토큰 생성
    	// 다른 API 요청 시 JWTFilter가 이 토큰을 검증하고 Authentication에 User를 넣어준다.
    	String token = jwtUtil.createToken(loginUser.getId(), loginUser.getEmail());
    	
    	Map<String, Object> data = Map.of(
        		"token", token,
        		"user", loginUser
        );
    	
    	return Response.success("로그인 되었습니다.", data);
    }
    
    // 로그아웃
    // JWT는 서버 세션을 사용하지 않으므로 서버에서 지울 로그인 상태가 없다.
    // 클라이언트가 저장해둔 JWT를 삭제하면 로그아웃 처리
    @PostMapping("/auth/logout")
    public Response<Void> logout() {
    	return Response.success("로그아웃 되었습니다.");
    }
    
    // 내 정보 조회
    @GetMapping("/users/me")
    public Response<User> getLoginUser(Authentication authentication) {    
    	User loginUser = getCurrentUser(authentication);
    	User user = userService.findById(loginUser.getId());
    	return Response.success(user);
    }
    
    // 내 정보 수정
    @PutMapping("/users/me")
    public Response<User> updateUser(@RequestBody User user, Authentication authentication) {
    	User loginUser = getCurrentUser(authentication);
    	User updated = userService.updateUser(loginUser.getId(), user);
    	return Response.success("회원 정보가 수정되었습니다.", updated);
    }
    
    // 회원 탈퇴
    @DeleteMapping("/users/me")
    public Response<Void> deleteUser(Authentication authentication) {
    	User loginUser = getCurrentUser(authentication);
    	userService.deleteUser(loginUser.getId());
    	return Response.success("회원 탈퇴가 완료되었습니다.");
    }
    
    // 현재 로그인 사용자 조회 공통 메서드
    // JwtFilter가 JWT 검증에 성공하면 Authentication의 principal에 User 객체를 넣어둔다.
    // 컨트롤러에서는 이 principal을 꺼내 현재 로그인 사용자를 판단한다.
    private User getCurrentUser(Authentication authentication) {
    	if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
    		throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
    	}    	
    	return (User) authentication.getPrincipal();
    }
    
}