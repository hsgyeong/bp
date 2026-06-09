package com.bp.jaringochi.domain.budget.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.budget.dto.WeeklyBudget;
import com.bp.jaringochi.domain.budget.service.BudgetService;
import com.bp.jaringochi.global.response.Response;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    // 4-1. 현재 주 예산 — GET /api/budgets/weekly/current
    @GetMapping("/weekly/current")
    public Response<WeeklyBudget> getCurrentWeek() {
        WeeklyBudget weeklyBudget = budgetService.getCurrentWeek(getCurrentUserId());
        return Response.success(weeklyBudget);
    }

    // 4-2. 이번 주 포함 최근 5주 — GET /api/budgets/weekly/recent
    @GetMapping("/weekly/recent")
    public Response<List<WeeklyBudget>> getRecentWeeks() {
        List<WeeklyBudget> weeks = budgetService.getRecentWeeks(getCurrentUserId());
        return Response.success(weeks);
    }

    // 4-3. 등록 — POST /api/budgets/weekly
    @PostMapping("/weekly")
    public Response<WeeklyBudget> addWeeklyBudget(@RequestBody WeeklyBudget weeklyBudget) {
        WeeklyBudget created = budgetService.addWeeklyBudget(getCurrentUserId(), weeklyBudget);
        return Response.success("주간 예산이 등록되었습니다.", created);
    }

    // 4-4. 수정 — PUT /api/budgets/weekly/{id}
    @PutMapping("/weekly/{id}")
    public Response<WeeklyBudget> updateWeeklyBudget(@PathVariable Long id,
                                                     @RequestBody WeeklyBudget weeklyBudget) {
        WeeklyBudget updated = budgetService.updateWeeklyBudget(getCurrentUserId(), id, weeklyBudget);
        return Response.success("주간 예산이 수정되었습니다.", updated);
    }

    // ===== 임시 인증 (TODO: 인증 적용 후 실제 로그인 사용자 id로 교체) =====
    private Long getCurrentUserId() {
        return 1L;
    }
}