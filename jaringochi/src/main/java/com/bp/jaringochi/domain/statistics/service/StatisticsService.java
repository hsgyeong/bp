package com.bp.jaringochi.domain.statistics.service;

import java.time.LocalDate;

import com.bp.jaringochi.domain.statistics.dto.CategoryStatistics;
import com.bp.jaringochi.domain.statistics.dto.StatisticsSummary;

public interface StatisticsService {

    // 6-1. 카테고리별 통계 (total + 각 항목 ratio). type 없으면 수입+지출 전체.
    CategoryStatistics getByCategory(Long userId, LocalDate startDate, LocalDate endDate, Integer type);

    // 6-2. 기간 수입/지출/잔액
    StatisticsSummary getSummary(Long userId, LocalDate startDate, LocalDate endDate);
}
