package com.bp.jaringochi.domain.statistics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bp.jaringochi.domain.statistics.dao.StatisticsDao;
import com.bp.jaringochi.domain.statistics.dto.CategoryStatItem;
import com.bp.jaringochi.domain.statistics.dto.CategoryStatistics;
import com.bp.jaringochi.domain.statistics.dto.MonthlyTrend;
import com.bp.jaringochi.domain.statistics.dto.MonthlyTrendItem;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final int TOP_N = 4;                       // 상위 4 + 기타
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private StatisticsDao statisticsDao;

    // 6-1. 카테고리별 통계: total 합산 -> 각 항목 ratio -> 상위 4 + 기타로 묶기
    @Override
    public CategoryStatistics getByCategory(Long userId, LocalDate startDate, LocalDate endDate, Integer type) {
        validateDates(startDate, endDate);

        List<CategoryStatItem> rows = statisticsDao.selectByCategory(userId, startDate, endDate, type);

        BigDecimal total = BigDecimal.ZERO;
        for (CategoryStatItem item : rows) {
            total = total.add(item.getAmount());
        }
        for (CategoryStatItem item : rows) {
            item.setRatio(calcRatio(item.getAmount(), total));
        }

        CategoryStatistics result = new CategoryStatistics();
        result.setTotal(total);
        result.setItems(collapseTail(rows, total));
        return result;
    }

    // 6-2. 월별 추이: 월 스파인 생성 -> 빈 달 0 채움 -> 전월대비
    @Override
    public MonthlyTrend getMonthlyTrend(Long userId, Integer type, Integer months) {
        if (type == null || months == null || months < 1) {
            throw new BusinessException(ErrorCode.STATISTICS_INVALID_INPUT);   // 400
        }

        YearMonth current = YearMonth.now();
        YearMonth start = current.minusMonths(months - 1L);
        LocalDate startDate = start.atDay(1);
        LocalDate endDate = current.atEndOfMonth();

        // 거래 있는 달만 -> Map으로
        List<MonthlyTrendItem> rows = statisticsDao.selectMonthlyTotals(userId, type, startDate, endDate);
        Map<String, BigDecimal> byMonth = new HashMap<>();
        for (MonthlyTrendItem r : rows) {
            byMonth.put(r.getMonth(), r.getAmount());
        }

        // start..current 월 스파인, 빈 달은 0
        List<MonthlyTrendItem> items = new ArrayList<>();
        YearMonth ym = start;
        for (int i = 0; i < months; i++) {
            String key = ym.format(MONTH_FMT);
            MonthlyTrendItem item = new MonthlyTrendItem();
            item.setMonth(key);
            item.setAmount(byMonth.getOrDefault(key, BigDecimal.ZERO));
            items.add(item);
            ym = ym.plusMonths(1);
        }

        MonthlyTrend result = new MonthlyTrend();
        result.setItems(items);
        result.setDiffRatio(calcDiffRatio(items));
        return result;
    }

    // ===== 상위 TOP_N 유지, 나머지를 '기타' 1건으로 합산 (rows는 금액 내림차순) =====
    private List<CategoryStatItem> collapseTail(List<CategoryStatItem> rows, BigDecimal total) {
        if (rows.size() <= TOP_N) {
            return rows;
        }
        List<CategoryStatItem> result = new ArrayList<>(rows.subList(0, TOP_N));

        BigDecimal etcAmount = BigDecimal.ZERO;
        for (CategoryStatItem item : rows.subList(TOP_N, rows.size())) {
            etcAmount = etcAmount.add(item.getAmount());
        }

        CategoryStatItem etc = new CategoryStatItem();
        etc.setCategoryId(null);
        etc.setCategoryName("기타");
        etc.setAmount(etcAmount);
        etc.setRatio(calcRatio(etcAmount, total));
        result.add(etc);
        return result;
    }

    // ===== 전월대비: (마지막 - 직전)/직전 × 100, 직전=0이면 null =====
    private BigDecimal calcDiffRatio(List<MonthlyTrendItem> items) {
        if (items.size() < 2) {
            return null;
        }
        BigDecimal last = items.get(items.size() - 1).getAmount();
        BigDecimal prev = items.get(items.size() - 2).getAmount();
        if (prev.signum() == 0) {
            return null;
        }
        return last.subtract(prev)
                   .multiply(BigDecimal.valueOf(100))
                   .divide(prev, 2, RoundingMode.HALF_UP);
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
