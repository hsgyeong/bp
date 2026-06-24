package com.bp.jaringochi.domain.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * 월간 레포트 — monthly_report 테이블 매핑 + 응답 본체.
 * 숫자(스냅샷)는 통계/예산 서비스로 계산하고, 텍스트는 AI(ChatClient)가 채운다.
 */
@Getter
@Setter
public class MonthlyReport {

    private Long id;

    @JsonIgnore                       // 응답에서 숨김 (소유권/insert 내부용)
    private Long userId;

    private Integer reportYear;
    private Integer reportMonth;

    // ===== 통계 스냅샷 (생성 시점 고정) =====
    private BigDecimal totalExpense;  // 이번 달 총 지출
    private BigDecimal prevExpense;   // 전월 총 지출
    private BigDecimal diffRatio;     // 전월 대비 %, 전월=0이면 null
    private Integer successWeeks;     // 완료된 최근 3주 중 ratio<=100 인 주 수
    private Integer totalWeeks;       // 집계 대상 주 수 (완료된 최근 3주, 최대 3)
    private String topCategory;       // 가장 많이 쓴 카테고리명

    @JsonIgnore                       // 원본 JSON 문자열은 숨기고, 아래 categories 로 노출
    private String categoryJson;

    @JsonIgnore                       // 원본 JSON 문자열은 숨기고, 아래 extra 로 노출
    private String extraJson;

    // ===== AI 생성 텍스트 =====
    private String oneLiner;          // 한 줄 총평
    private String mood;              // happy|smirk|angry|sad (코드가 결정, 4종)
    private String categoryComment;   // 카테고리 분석 코멘트
    private String advice;            // 다음 달 조언
    private String story;             // 굴비의 총평 (긴 이야기, 300자 내외)

    // ===== 굴비에게 한 마디 (월 1회) =====
    private String userMessage;
    private String gulbiReply;
    private LocalDateTime repliedAt;

    private LocalDateTime generatedAt;

    // ===== 컬럼이 아닌 파생 필드 (JSON 을 파싱해 응답에 실어줌) =====
    private List<CategoryDiffItem> categories;   // categoryJson 파싱
    private ReportExtra extra;                    // extraJson 파싱 (부가 지표)
}
