package com.bp.jaringochi.domain.statistics.dao;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.statistics.dto.CategoryStatItem;
import com.bp.jaringochi.domain.statistics.dto.StatisticsSummary;

@Mapper
public interface StatisticsDao {

    // 6-1. 카테고리별 집계 (금액 내림차순). type 없으면 수입+지출 전체.
    List<CategoryStatItem> selectByCategory(@Param("userId") Long userId,
                                            @Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate,
                                            @Param("type") Integer type);

    // 6-2. 기간 수입/지출 합계 (balance는 서비스에서 뺄셈)
    StatisticsSummary selectSummary(@Param("userId") Long userId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);
}
