package com.bp.jaringochi.domain.statistics.service;

import java.time.LocalDate;
import java.util.List;

import com.bp.jaringochi.domain.statistics.dto.CategoryStatistics;
import com.bp.jaringochi.domain.statistics.dto.DailyExpense;
import com.bp.jaringochi.domain.statistics.dto.MonthlyTrend;

public interface StatisticsService {

    // 6-1. 카테고리별 통계 (상위 4 + 기타). type 없으면 수입+지출 전체.
    CategoryStatistics getByCategory(Long userId, LocalDate startDate, LocalDate endDate, Integer type);

    // 6-2. 월별 추이 (최근 months개월 + 전월대비)
    MonthlyTrend getMonthlyTrend(Long userId, Integer type, Integer months);

    // 레포트용: 일자별 지출 합계 (거래 있는 날만)
    List<DailyExpense> getDailyExpense(Long userId, LocalDate startDate, LocalDate endDate);
}
