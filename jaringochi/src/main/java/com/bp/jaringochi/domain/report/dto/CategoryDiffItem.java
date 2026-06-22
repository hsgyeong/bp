package com.bp.jaringochi.domain.report.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 카테고리 분석 항목 — MonthlyReport.categoryJson 의 직렬화 구조.
 * 이번 달 금액/비율 + 전월 대비 증감액을 담는다.
 */
@Getter
@Setter
public class CategoryDiffItem {
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;      // 이번 달 그 카테고리 지출
    private BigDecimal ratio;       // 이번 달 지출 중 비율 %
    private BigDecimal diffAmount;  // 전월 대비 증감액 (양수=늘어남), 전월 없으면 amount 그대로
}
