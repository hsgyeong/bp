package com.bp.jaringochi.domain.report.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * "굴비가 기억하는 너" — 과거 레포트에서 사용자가 굴비에게 남긴 가장 최근 다짐.
 * 저장 컬럼이 아니라 조회 시점에 과거 레포트에서 끌어와 응답에 실어준다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReportMemory {
    private Integer reportYear;
    private Integer reportMonth;
    private String userMessage;   // 그때 사용자가 한 다짐
    private String gulbiReply;    // 그때 굴비의 답
}
