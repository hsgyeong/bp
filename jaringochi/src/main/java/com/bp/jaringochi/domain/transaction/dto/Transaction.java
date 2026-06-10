package com.bp.jaringochi.domain.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Transaction {
	
	private Long id;
	private Long userId;
	private Long categoryId;
	private BigDecimal amount;
	private int type;	// 1=수입, 2=지출
	private String memo;
	private LocalDate date;
	
}
