package com.bp.jaringochi.domain.budget.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.budget.dto.WeeklyBudget;
import com.bp.jaringochi.domain.budget.service.BudgetService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.bp.jaringochi.global.response.Response;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    // 4-1. 현재 주 예산
    @GetMapping("/weekly/current")
    public Response<WeeklyBudget> getCurrentWeek(Authentication authentication) {
        WeeklyBudget weeklyBudget = budgetService.getCurrentWeek(getCurrentUserId(authentication));
        return Response.success(weeklyBudget);
    }

    // 4-2. 최근 5주
    @GetMapping("/weekly/recent")
    public Response<List<WeeklyBudget>> getRecentWeeks(Authentication authentication) {
        List<WeeklyBudget> weeks = budgetService.getRecentWeeks(getCurrentUserId(authentication));
        return Response.success(weeks);
    }

    // 4-3. 등록
    @PostMapping("/weekly")
    public Response<WeeklyBudget> addWeeklyBudget(@RequestBody WeeklyBudget weeklyBudget,
                                                  Authentication authentication) {
        WeeklyBudget created = budgetService.addWeeklyBudget(getCurrentUserId(authentication), weeklyBudget);
        return Response.success("주간 예산이 등록되었습니다.", created);
    }

    // 4-4. 수정
    @PutMapping("/weekly/{id}")
    public Response<WeeklyBudget> updateWeeklyBudget(@PathVariable Long id,
                                                     @RequestBody WeeklyBudget weeklyBudget,
                                                     Authentication authentication) {
        WeeklyBudget updated = budgetService.updateWeeklyBudget(getCurrentUserId(authentication), id, weeklyBudget);
        return Response.success("주간 예산이 수정되었습니다.", updated);
    }

    // ===== 토큰에서 userId 추출 (거래 컨트롤러와 동일 패턴) =====
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
        }
        return Long.valueOf(jwt.getSubject());
    }
}