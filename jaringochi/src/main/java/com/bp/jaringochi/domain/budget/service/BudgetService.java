package com.bp.jaringochi.domain.budget.service;

import java.util.List;

import com.bp.jaringochi.domain.budget.dto.WeeklyBudget;

public interface BudgetService {
	
	// userId=현재 유저, req=요청 바디(amount/startDate/endDate), id=경로변수

    // 4-1. 현재 주 예산 (지출 현황 포함)
    WeeklyBudget getCurrentWeek(Long userId);

    // 4-2. 이번 주 포함 최근 5주
    List<WeeklyBudget> getRecentWeeks(Long userId);

    // 4-3. 주간 예산 등록
    WeeklyBudget addWeeklyBudget(Long userId, WeeklyBudget req);

    // 4-4. 주간 예산 수정
    WeeklyBudget updateWeeklyBudget(Long userId, Long id, WeeklyBudget req);
}