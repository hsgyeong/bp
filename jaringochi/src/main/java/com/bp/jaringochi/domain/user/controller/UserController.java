package com.bp.jaringochi.domain.user.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	
	private final UserService userService;

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
    private User getCurrentUser(Authentication authentication) {
    	if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
    		throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
    	}    	
    	
    	Long userId = Long.valueOf(jwt.getSubject());
    	return userService.findById(userId);
    }
    
}