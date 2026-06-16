package com.bp.jaringochi.domain.statistics.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.statistics.dto.CategoryStatistics;
import com.bp.jaringochi.domain.statistics.dto.MonthlyTrend;
import com.bp.jaringochi.domain.statistics.service.StatisticsService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.bp.jaringochi.global.response.Response;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
public class StatisticsController {

	private final StatisticsService statisticsService;

	// 6-1. 카테고리별 통계 (상위 4 + 기타, type 옵션). 월/주 카테고리별 공용.
	@GetMapping("/by-category")
	public Response<CategoryStatistics> getByCategory(
			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate startDate,

			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate endDate,

			@RequestParam(required = false)
			Integer type,

			Authentication authentication) {

		Long userId = getCurrentUserId(authentication);

		CategoryStatistics result = statisticsService.getByCategory(userId, startDate, endDate, type);
		return Response.success(result);
	}

	// 6-2. 월별 추이 (최근 months개월 + 전월대비). type 필수.
	@GetMapping("/monthly-trend")
	public Response<MonthlyTrend> getMonthlyTrend(
			@RequestParam Integer type,
			@RequestParam(defaultValue = "6") Integer months,
			Authentication authentication) {

		Long userId = getCurrentUserId(authentication);

		MonthlyTrend result = statisticsService.getMonthlyTrend(userId, type, months);
		return Response.success(result);
	}

	private Long getCurrentUserId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
		}

		return Long.valueOf(jwt.getSubject());
	}

}
