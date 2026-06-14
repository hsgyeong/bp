package com.bp.jaringochi.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.user.dto.LoginRequest;
import com.bp.jaringochi.domain.user.dto.LoginResponse;
import com.bp.jaringochi.domain.user.dto.User;
import com.bp.jaringochi.domain.user.service.AuthService;
import com.bp.jaringochi.domain.user.service.UserService;
import com.bp.jaringochi.global.response.Response;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserService userService;
	
	@Operation(summary = "로그인", description = "email/password 인증 후 Access Token(JWT)을 발급")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
	
	// 회원가입
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Response<User> signup(@RequestBody User user) {
    	User created = userService.signup(user);
    	return Response.success("회원가입이 완료되었습니다.", created);
    }
    
    // 로그아웃
    // JWT는 서버 세션을 사용하지 않으므로 서버에서 지울 로그인 상태가 없다.
    // 클라이언트가 저장해둔 JWT를 삭제하면 로그아웃 처리
    @PostMapping("/logout")
    public Response<Void> logout() {
    	return Response.success("로그아웃 되었습니다.");
    }
    
    // 닉네임 확인
    @GetMapping("/check-nickname")
    public Response<Boolean> checkNickname(@RequestParam String nickname) {
        boolean available = userService.isNicknameAvailable(nickname);
        return Response.success(available);
    }

}
