package com.bp.jaringochi.domain.report.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.report.dto.MonthlyReport;
import com.bp.jaringochi.domain.report.dto.TalkRequest;
import com.bp.jaringochi.domain.report.service.ReportService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.bp.jaringochi.global.response.Response;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    // 월간 레포트 조회 (없으면 생성·저장 후 반환)
    @GetMapping("/monthly")
    public Response<MonthlyReport> getMonthly(@RequestParam Integer year,
                                              @RequestParam Integer month,
                                              Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return Response.success(reportService.getMonthly(userId, year, month));
    }

    // 굴비에게 한 마디 (월 1회)
    @PostMapping("/monthly/talk")
    public Response<MonthlyReport> talk(@RequestBody TalkRequest req,
                                        Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        MonthlyReport result = reportService.talk(userId, req.getYear(), req.getMonth(), req.getMessage());
        return Response.success("굴비가 한마디 남겼어요.", result);
    }

    // ===== 토큰에서 userId 추출 (통계/예산 컨트롤러와 동일 패턴) =====
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
        }
        return Long.valueOf(jwt.getSubject());
    }
}
