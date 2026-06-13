package com.bp.jaringochi.domain.statistics.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryStatItem {
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;   // 그 카테고리 합계 (SQL에서 채움)
    private BigDecimal ratio;    // total 대비 % (서비스에서 채움)
}
