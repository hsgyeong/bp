package com.bp.jaringochi.domain.statistics.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthlyTrend {
    private List<MonthlyTrendItem> items;  // 과거->현재 순, 빈 달 0 채움
    private BigDecimal diffRatio;          // 마지막달 vs 직전달 % (음수=절약), 직전=0이면 null
}
