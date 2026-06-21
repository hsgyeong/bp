package com.bp.jaringochi.domain.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 월간 레포트. monthly_report 테이블 매핑.
 * - 통계 스냅샷(숫자)은 생성 시점에 고정 저장
 * - one_liner/mood/category_comment/advice 는 OpenAI 생성 텍스트
 * - user_message/gulbi_reply 는 "굴비에게 한 마디"(월 1회) 결과
 */
@Getter
@Setter
public class MonthlyReport {

    private Long id;

    @JsonIgnore                  // 응답에서 숨김 (소유권/insert 내부용)
    private Long userId;

    private Integer reportYear;
    private Integer reportMonth;

    // ===== 통계 스냅샷 =====
    private BigDecimal totalExpense;
    private BigDecimal prevExpense;
    private BigDecimal diffRatio;     // 전월 대비 %, prev=0이면 null
    private Integer successWeeks;
    private Integer totalWeeks;
    private String topCategory;

    @JsonIgnore                  // DB엔 JSON 문자열로, 프론트엔는 categories(파싱본)로 노출
    private String categoryJson;

    // ===== AI 생성 텍스트 =====
    private String oneLiner;
    private String mood;          // hello|warn|happy|sad|hungry|sulk|angry
    private String categoryComment;
    private String advice;

    // ===== 굴비에게 한 마디 (월 1회) =====
    private String userMessage;
    private String gulbiReply;
    private LocalDateTime repliedAt;

    private LocalDateTime generatedAt;

    // 컬럼 아님: categoryJson 을 파싱해 서비스에서 채워 응답한다.
    private List<CategoryDiffItem> categories;
}
