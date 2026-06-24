package com.bp.jaringochi.domain.budget.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.budget.dao.BudgetDao;
import com.bp.jaringochi.domain.budget.dto.WeeklyBudget;
import com.bp.jaringochi.domain.notification.service.NotificationService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

@Service
public class BudgetServiceImpl implements BudgetService {

    @Autowired
    private BudgetDao budgetDao;

    @Autowired
    private NotificationService notificationService;

    // 4-1. 현재 주 예산
    @Override
    public WeeklyBudget getCurrentWeek(Long userId) {
        WeeklyBudget weeklyBudget = budgetDao.selectCurrentWeek(userId);
        if (weeklyBudget == null) {
            throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);          // 404
        }
        weeklyBudget.setRatio(calcRatio(weeklyBudget.getSpentMoney(), weeklyBudget.getAmount()));
        return weeklyBudget;
    }

    // 4-2. 이번 주 포함 최근 5주
    @Override
    public List<WeeklyBudget> getRecentWeeks(Long userId) {
        List<WeeklyBudget> list = budgetDao.selectRecentWeeks(userId);        // 최신순 5개
        for (WeeklyBudget wb : list) {
            wb.setRatio(calcRatio(wb.getSpentMoney(), wb.getAmount()));
        }
        Collections.reverse(list);                                           // 과거→현재 순 (API.md)
        return list;
    }

    // 레포트용: 해당 월에 걸치는 주들 (과거→현재, ratio 채움)
    @Override
    public List<WeeklyBudget> getWeeksByMonth(Long userId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<WeeklyBudget> list = budgetDao.selectWeeksByMonth(userId, startDate, endDate);
        for (WeeklyBudget wb : list) {
            wb.setRatio(calcRatio(wb.getSpentMoney(), wb.getAmount()));
        }
        return list;
    }

    // 4-3. 등록
    @Override
    @Transactional
    public WeeklyBudget addWeeklyBudget(Long userId, WeeklyBudget weeklyBudget) {
        validateInput(weeklyBudget);                                         // 400
        weeklyBudget.setUserId(userId);                                      // 내 예산으로

        WeeklyBudget dup = budgetDao.selectByWeek(userId, weeklyBudget.getStartDate());
        if (dup != null) {
            throw new BusinessException(ErrorCode.BUDGET_ALREADY_EXISTS);     // 409
        }

        budgetDao.insertWeeklyBudget(weeklyBudget);                          // id 채워짐
        WeeklyBudget created = budgetDao.selectById(weeklyBudget.getId());
        created.setRatio(calcRatio(created.getSpentMoney(), created.getAmount()));
        return created;
    }

    // 4-4. 수정 (월 2회 제한)
    @Override
    @Transactional
    public WeeklyBudget updateWeeklyBudget(Long userId, Long id, WeeklyBudget weeklyBudget) {
        WeeklyBudget existing = findOwned(userId, id);                       // 404 + 403(소유권)
        if (!existing.isUpdatable()) {
            throw new BusinessException(ErrorCode.BUDGET_UPDATE_LIMIT_EXCEEDED); // 403
        }

        weeklyBudget.setId(id);
        budgetDao.updateWeeklyBudget(weeklyBudget);                          // amount + update_count +1
        WeeklyBudget updated = budgetDao.selectById(id);
        updated.setRatio(calcRatio(updated.getSpentMoney(), updated.getAmount()));

        // 예산 기준이 바뀌었으므로 그 주 알림 리셋 후 새 금액 기준으로 재평가 (DEC-0017)
        notificationService.reevaluateOnBudgetChange(userId, id, updated.getStartDate());

        return updated;
    }

    // ===== 공통 검증: 존재 + 소유권 =====
    private WeeklyBudget findOwned(Long userId, Long id) {
        WeeklyBudget weeklyBudget = budgetDao.selectById(id);
        if (weeklyBudget == null) {
            throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);          // 404
        }
//        if (weeklyBudget.getUserId() == null || !weeklyBudget.getUserId().equals(userId)) {
//            throw new BusinessException(ErrorCode.BUDGET_FORBIDDEN);          // 403 <- ErrorCode 추가 필요 : 논의해서 정해야 함!
//        }
        
        if (weeklyBudget.getUserId() == null || !weeklyBudget.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.USER_FORBIDDEN);     // U403 "권한이 없습니다." (임시)
        }
        
        return weeklyBudget;
    }

    // ===== 비율 계산: spent/amount×100, 2자리 반올림, 0 가드 =====
    private BigDecimal calcRatio(BigDecimal spent, BigDecimal amount) {
        if (amount == null || amount.signum() == 0 || spent == null) {
            return BigDecimal.ZERO;
        }
        return spent.multiply(BigDecimal.valueOf(100))
                    .divide(amount, 2, RoundingMode.HALF_UP);
    }

    // ===== 입력 검증 (4-3) =====
    private void validateInput(WeeklyBudget weeklyBudget) {
        if (weeklyBudget.getAmount() == null || weeklyBudget.getAmount().signum() <= 0
                || weeklyBudget.getStartDate() == null || weeklyBudget.getEndDate() == null
                || weeklyBudget.getEndDate().isBefore(weeklyBudget.getStartDate())) {
            throw new BusinessException(ErrorCode.BUDGET_INVALID_INPUT);      // 400
        }
    }
}