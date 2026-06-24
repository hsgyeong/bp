package com.bp.jaringochi.domain.report.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 카테고리 분석 항목 — MonthlyReport.categoryJson 의 직렬화 구조.
 * 이번 달 금액/비율 + 전월 금액/비율 + 전월 대비 증감액을 담는다.
 * (전월/당월 도넛 2개를 같은 리스트로 그리기 위해 전월 값도 함께 보관)
 */
@Getter
@Setter
public class CategoryDiffItem {
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;      // 이번 달 그 카테고리 지출 (당월에 없으면 0)
    private BigDecimal ratio;       // 이번 달 지출 중 비율 % (당월에 없으면 0)
    private BigDecimal prevAmount;  // 전월 그 카테고리 지출 (전월에 없으면 0)
    private BigDecimal prevRatio;   // 전월 지출 중 비율 % (전월에 없으면 0)
    private BigDecimal diffAmount;  // 전월 대비 증감액 (양수=늘어남) = amount - prevAmount
}
