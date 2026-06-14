package com.bp.jaringochi.domain.statistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bp.jaringochi.domain.statistics.dao.StatisticsDao;
import com.bp.jaringochi.domain.statistics.dto.CategoryStatItem;
import com.bp.jaringochi.domain.statistics.dto.CategoryStatistics;
import com.bp.jaringochi.domain.statistics.dto.StatisticsSummary;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private StatisticsDao statisticsDao;

    // 6-1. 카테고리별 통계: total 합산 -> 각 항목 ratio 계산
    @Override
    public CategoryStatistics getByCategory(Long userId, LocalDate startDate, LocalDate endDate, Integer type) {
        validateDates(startDate, endDate);

        List<CategoryStatItem> items = statisticsDao.selectByCategory(userId, startDate, endDate, type);

        // 1차 순회: 분모(total) 완성
        BigDecimal total = BigDecimal.ZERO;
        for (CategoryStatItem item : items) {
            total = total.add(item.getAmount());
        }
        // 2차 순회: total 대비 비율 채움 (분모가 있어야 가능하므로 분리)
        for (CategoryStatItem item : items) {
            item.setRatio(calcRatio(item.getAmount(), total));
        }

        CategoryStatistics result = new CategoryStatistics();
        result.setTotal(total);
        result.setItems(items);
        return result;
    }

    // 6-2. 기간 수입/지출/잔액: balance = income - expense
    @Override
    public StatisticsSummary getSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);

        StatisticsSummary summary = statisticsDao.selectSummary(userId, startDate, endDate);
        // income/expense는 Mapper의 COALESCE(...,0) 덕에 null 아님 -> 바로 뺄셈
        summary.setBalance(summary.getIncome().subtract(summary.getExpense()));
        return summary;
    }

    // ===== 비율: amount/total×100, 2자리 반올림, total=0 가드 =====
    private BigDecimal calcRatio(BigDecimal amount, BigDecimal total) {
        if (total == null || total.signum() == 0 || amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100))
                     .divide(total, 2, RoundingMode.HALF_UP);
    }

    // ===== 날짜 검증: null 또는 end < start -> 400 =====
    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.STATISTICS_INVALID_INPUT);
        }
    }
}
