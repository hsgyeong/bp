package com.bp.jaringochi.domain.statistics.dao;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.statistics.dto.CategoryStatItem;
import com.bp.jaringochi.domain.statistics.dto.DailyExpense;
import com.bp.jaringochi.domain.statistics.dto.MonthlyTrendItem;

@Mapper
public interface StatisticsDao {

    // 레포트용: 일자별 지출 합계 (type=2, 기간 내, 거래 있는 날만). 무지출일·최대일 산정
    List<DailyExpense> selectDailyExpense(@Param("userId") Long userId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    // 6-1. 카테고리별 집계 (금액 내림차순). type 없으면 수입+지출 전체.
    List<CategoryStatItem> selectByCategory(@Param("userId") Long userId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("type") Integer type);

    // 6-2. 월별 합계 (거래 있는 달만 'yyyy-MM'으로). 빈 달 채움/전월대비는 서비스에서.
    List<MonthlyTrendItem> selectMonthlyTotals(@Param("userId") Long userId,
                                               @Param("type") Integer type,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
}
