package com.bp.jaringochi.domain.statistics.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryStatistics {
    private BigDecimal total;              // items 합계 (서비스에서 계산)
    private List<CategoryStatItem> items;  // 금액 내림차순
}
