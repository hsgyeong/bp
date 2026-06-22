package com.bp.jaringochi.domain.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeeklyBudget {


    private Long id;                 

    @JsonIgnore                      // 응답에서 숨김 (소유권/insert 내부용)
    private Long userId;             

    private BigDecimal amount;       
    private LocalDate startDate;    
    private LocalDate endDate;       
    private Integer updateCount;     // 월 2회 제한 판단 위해
    private BigDecimal spentMoney;   
    private BigDecimal ratio;        // spentMoney/amount×100 Service에서 반올림해서 입력
    private String rewardStatus;	 // NULL=미뽑기 / PENDING / ACCEPTED / DECLINED

    // 필드엔 없는데 만든 getter
    public boolean isUpdatable() {
        return updateCount != null && updateCount < 2;
    }

    public BigDecimal getRemaining() {
        if (amount == null || spentMoney == null) return null;
        return amount.subtract(spentMoney);
    }
}