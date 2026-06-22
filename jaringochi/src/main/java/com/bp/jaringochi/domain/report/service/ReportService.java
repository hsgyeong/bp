package com.bp.jaringochi.domain.report.service;

import com.bp.jaringochi.domain.report.dto.MonthlyReport;

public interface ReportService {

    // 해당 월 레포트 조회 — 없으면 생성·저장 후 반환 (월 1회)
    MonthlyReport getMonthly(Long userId, int year, int month);

    // 굴비에게 한 마디 — 응답 저장 후 갱신된 레포트 반환 (월 1회)
    MonthlyReport talk(Long userId, int year, int month, String message);
}
