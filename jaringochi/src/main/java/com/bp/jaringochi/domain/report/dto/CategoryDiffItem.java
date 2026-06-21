package com.bp.jaringochi.domain.report.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 월간 레포트의 카테고리별 분석 한 줄.
 * 이번 달 금액·비율 + 전월 대비 증감을 함께 담아 category_json(TEXT)으로 직렬화 저장한다.
 */
@Getter
@Setter
public class CategoryDiffItem {
    private String categoryName;   // 카테고리명 ("기타" 포함)
    private BigDecimal amount;     // 이번 달 지출
    private BigDecimal ratio;      // 이번 달 total 대비 %
    private BigDecimal prevAmount; // 전월 같은 카테고리 지출 (없으면 0)
    private BigDecimal diffAmount; // amount - prevAmount (+면 늘어남)
}
