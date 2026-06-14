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
import com.bp.jaringochi.domain.user.dto.LogoutRequest;
import com.bp.jaringochi.domain.user.dto.User;
import com.bp.jaringochi.domain.user.service.AuthService;
import com.bp.jaringochi.domain.user.service.RefreshTokenService;
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
	private final RefreshTokenService refreshTokenService;
	
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
    
    @PostMapping("/logout")
    public Response<Void> logout(@Valid @RequestBody LogoutRequest request) {
        // 클라이언트가 보낸 refreshToken을 DB에서 폐기한다.
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());

        return Response.success("로그아웃 되었습니다.");
    }
    
    // 닉네임 확인
    @GetMapping("/check-nickname")
    public Response<Boolean> checkNickname(@RequestParam String nickname) {
        boolean available = userService.isNicknameAvailable(nickname);
        return Response.success(available);
    }

}
