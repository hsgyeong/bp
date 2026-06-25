package com.bp.jaringochi.domain.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Notification {

    private Long id;

    @JsonIgnore                       // 소유권 확인용, 응답엔 숨김
    private Long userId;

    private String type;               // BUDGET / DRAW / REPORT

    // DRAW는 뽑기 화면 이동에 weeklyBudgetId가 필요해 응답에 노출 (BUDGET도 내부 id라 무해)
    private Long weeklyBudgetId;       // BUDGET/DRAW 전용, REPORT는 null

    private Integer threshold;         // 25/50/75/100/125/150 (BUDGET 전용)
    private BigDecimal currentBudget;  // 그 시점 예산 (스냅샷, BUDGET)
    private BigDecimal spentMoney;     // 그 시점 지출 합 (스냅샷, BUDGET)
    private BigDecimal ratio;          // % (BUDGET)
    private Integer reportYear;        // 레포트 대상 연도 (REPORT 전용)
    private Integer reportMonth;       // 레포트 대상 월 (REPORT 전용)
    private Integer isRead;            // 0=안읽음 / 1=읽음
    private LocalDateTime createdAt;
}