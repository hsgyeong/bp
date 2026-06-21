package com.bp.jaringochi.domain.gulbi.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.gulbi.dto.GulbiDecisionRequest;
import com.bp.jaringochi.domain.gulbi.dto.GulbiDrawRequest;
import com.bp.jaringochi.domain.gulbi.dto.GulbiDrawResponse;
import com.bp.jaringochi.domain.gulbi.service.GulbiRewardService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.bp.jaringochi.global.response.Response;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping("/api/budgets/weekly/{weeklyBudgetId}/gulbi-reward")
public class GulbiRewardController {

	private final GulbiRewardService gulbiRewardService;
	
	public GulbiRewardController(GulbiRewardService gulbiRewardService) {
		this.gulbiRewardService = gulbiRewardService;
	}
	
	@PostMapping("/draw")
	public Response<GulbiDrawResponse> draw(
			@PathVariable Long weeklyBudgetId,
			@RequestBody GulbiDrawRequest request,
			Authentication authentication
	) {
		Long userId = getCurrentUserId(authentication);
		return Response.success(gulbiRewardService.draw(userId, weeklyBudgetId, request));
	}
	
	@PostMapping("/decision")
	public Response<Void> decide(
		@PathVariable Long weeklyBudgetId,
		@RequestBody GulbiDecisionRequest request,
		Authentication authentication
	) {
		Long userId = getCurrentUserId(authentication);
		gulbiRewardService.decide(userId, weeklyBudgetId, request.decision());
		return Response.success("굴비 옷 선택이 반영되었습니다.", null);
	}
	
	private Long getCurrentUserId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
		}
		return Long.valueOf(jwt.getSubject());
	}
	
}
