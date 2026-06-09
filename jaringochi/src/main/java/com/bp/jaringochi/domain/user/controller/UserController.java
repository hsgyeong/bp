package com.bp.jaringochi.domain.user.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.config.jwt.JwtUtil;
import com.bp.jaringochi.domain.user.dto.User;
import com.bp.jaringochi.domain.user.service.UserService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
	
	private final JwtUtil jwtUtil;	
	private final UserService userService;

    @PostMapping("/auth/signup")
    public User signup(@RequestBody User user) {
    	return userService.signup(user);
    }
    
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody User user) {
    	User loginUser = userService.login(user.getEmail(), user.getPassword());
    	
    	String token = jwtUtil.createToken(loginUser.getId(), loginUser.getPassword());
    	
    	return Map.of(
    		"token", token,
    		"user", loginUser
    	);
    }
    
    @PostMapping("/auth/logout")
    public Map<String, String> logout() {
    	return Map.of("message", "클라이언트에서 JWT 토큰을 삭제하면 로그아웃됩니다.");
    }
    
    @GetMapping("/users/me")
    public User getLoginUser(Authentication authentication) {    	
    	return getCurrentUser(authentication);
    }
    
    @GetMapping("/users/{id}")
    public User findById(@PathVariable Long id) {
    	return userService.findById(id);
    }
    
    @GetMapping("/users/email/{email}")
    public User findByEmail(@PathVariable String email) {
    	return userService.findByEmail(email);
    }
    
    @PutMapping("/users/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user, Authentication authentication) {
    	User loginUser = getCurrentUser(authentication);
    	
    	if (!loginUser.getId().equals(id)) {
    		throw new BusinessException(ErrorCode.USER_FORBIDDEN);
    	}
    	return userService.updateUser(id, user);
    }
    
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Long id, Authentication authentication) {
    	User loginUser = getCurrentUser(authentication);

		if (!loginUser.getId().equals(id)) {
			throw new BusinessException(ErrorCode.USER_FORBIDDEN);
		}
    	userService.deleteUser(id);
    }
    
    private User getCurrentUser(Authentication authentication) {
    	if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
    		throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
    	}
    	
    	return (User) authentication.getPrincipal();
    }
    
}