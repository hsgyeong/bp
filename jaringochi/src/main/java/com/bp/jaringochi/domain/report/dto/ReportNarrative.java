package com.bp.jaringochi.domain.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 구조화 출력 전용 DTO.
 * ChatClient 의 {@code .call().entity(ReportNarrative.class)} 로 역직렬화된다.
 * (Spring AI 가 이 클래스로부터 JSON 스키마를 만들어 모델에 형식을 강제한다.)
 */
@Getter
@Setter
@NoArgsConstructor
public class ReportNarrative {
    private String oneLiner;          // 한 줄 총평 (40자 이내)
    private String mood;              // hello|warn|happy|sad|hungry|sulk|angry 중 하나
    private String categoryComment;   // 카테고리 분석 코멘트 (80자 이내)
    private String advice;            // 다음 달 조언 (100자 이내)
}
