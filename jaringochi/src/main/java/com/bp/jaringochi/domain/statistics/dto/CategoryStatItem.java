package com.bp.jaringochi.domain.statistics.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryStatItem {
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;   // 그 카테고리 합계 (SQL에서 채움)
    private BigDecimal ratio;    // total 대비 % (서비스에서 채움)

    // '기타'로 합쳐진 세부 카테고리 목록 (그 외 항목은 null) — 레포트에서 '기타' 펼쳐보기용
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CategoryStatItem> members;
}
