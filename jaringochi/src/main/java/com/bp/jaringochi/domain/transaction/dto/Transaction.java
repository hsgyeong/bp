package com.bp.jaringochi.domain.transaction.dto;

import java.math.BigDecimal;

public class Transaction {
	
	private Long id;
	private Long userId;
	private Long categoryId;
	private BigDecimal amount;
	private int type;
	private String memo;
	
}
