package com.bp.jaringochi.domain.statistics.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthlyTrendItem {
    private String month;       // "yyyy-MM" (SQL DATE_FORMAT)
    private BigDecimal amount;  // 그 달 합계 (빈 달은 서비스가 0으로 채움)
}
