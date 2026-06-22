package com.bp.jaringochi.domain.gulbi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GulbiDrawContext {
	private BigDecimal amount;		// 그 주 예산
	private BigDecimal spentMoney;	// 그 주 지출 합
	private LocalDate endDate;		// 주 종료일 (끝난 주인지 판정)
	private String rewardStatus;	// NULL / PENDING / ACCEPTED / DECLINED
	private String currentOutfitKey;// 현재 입은 옷 (중복 뽑기 방지)
}
