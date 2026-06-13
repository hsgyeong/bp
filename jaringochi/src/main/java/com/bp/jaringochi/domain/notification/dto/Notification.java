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

    @JsonIgnore                       // 내부 참조용, 응답엔 숨김
    private Long weeklyBudgetId;

    private Integer threshold;         // 25/50/75/100/125/150
    private BigDecimal currentBudget;  // 그 시점 예산 (스냅샷)
    private BigDecimal spentMoney;     // 그 시점 지출 합 (스냅샷)
    private BigDecimal ratio;          // %
    private Integer isRead;            // 0=안읽음 / 1=읽음
    private LocalDateTime createdAt;
}