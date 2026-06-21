package com.bp.jaringochi.domain.report.service;

import com.bp.jaringochi.domain.report.dto.MonthlyReport;

public interface ReportService {

    // 해당 월 레포트 조회 — 없으면 생성·저장 후 반환 (월 1회 생성)
    MonthlyReport getOrCreate(Long userId, Integer year, Integer month);

    // "굴비에게 한 마디" — 해당 월 레포트에 사용자 메시지/굴비 응답 저장 (월 1회)
    MonthlyReport talk(Long userId, Integer year, Integer month, String message);
}
