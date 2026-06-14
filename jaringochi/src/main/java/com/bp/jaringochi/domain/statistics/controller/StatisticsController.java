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
import com.bp.jaringochi.domain.statistics.dto.StatisticsSummary;
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

	// 6-1. 카테고리별 통계 (type 옵션)
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

	// 6-2. 기간 수입/지출/잔액
	@GetMapping("/summary")
	public Response<StatisticsSummary> getSummary(
			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate startDate,

			@RequestParam
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate endDate,

			Authentication authentication) {

		Long userId = getCurrentUserId(authentication);

		StatisticsSummary summary = statisticsService.getSummary(userId, startDate, endDate);
		return Response.success(summary);
	}

	private Long getCurrentUserId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
		}

		return Long.valueOf(jwt.getSubject());
	}

}
