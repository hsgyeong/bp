package com.bp.jaringochi.domain.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

/**
 * 일자별 지출 합계 — 무지출 날 수·가장 큰 하루 산정용.
 * 거래가 있는 날만 반환된다(없는 날은 row 자체가 없음).
 */
@Getter
@Setter
public class DailyExpense {
    private LocalDate date;
    private BigDecimal amount;   // 그 날 지출 합계
}
