package com.bp.jaringochi.domain.notification.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.notification.dao.NotificationDao;
import com.bp.jaringochi.domain.notification.dto.Notification;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import org.springframework.dao.DuplicateKeyException;

import com.bp.jaringochi.domain.budget.dao.BudgetDao;
import com.bp.jaringochi.domain.budget.dto.WeeklyBudget;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationDao notificationDao;
    
    @Autowired
    private BudgetDao budgetDao;

    // 5-1. 목록 - 그대로 위임
    @Override
    public List<Notification> getNotifications(Long userId, Integer isRead) {
        return notificationDao.selectByUser(userId, isRead);
    }

    // 5-2. 개수 - 그대로 위임
    @Override
    public int getUnreadCount(Long userId) {
        return notificationDao.countUnread(userId);
    }

    // 5-3. 단건 읽음 - 소유권 확인 후 처리
    @Override
    @Transactional
    public void markAsRead(Long userId, Long id) {
        findOwned(userId, id);              // 404 + 403
        notificationDao.markRead(id);
    }

    // 5-4. 전체 읽음 - 내 것만 한 방에
    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationDao.markAllRead(userId);
    }

    // ===== 공통: 존재 + 소유권 검증 =====
    private Notification findOwned(Long userId, Long id) {
        Notification n = notificationDao.selectById(id);
        if (n == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);   // 404
        }
        if (n.getUserId() == null || !n.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_FORBIDDEN);   // 403
        }
        return n;
    }
    
    // 임계 단계 (오름차순)
    private static final int[] THRESHOLDS = {25, 50, 75, 100, 125, 150};

    // 트리거: 지출 등록 직후 호출됨. @Transactional 의도적으로 없음(위 설계 포인트 참고)
    @Override
    public void evaluateExpense(Long userId, LocalDate date) {
        try {
            WeeklyBudget wb = budgetDao.selectByDate(userId, date);
            if (wb == null) {
                return;                                   // 예산 안 짠 주 -> 평가 대상 아님
            }

            BigDecimal ratio = calcRatio(wb.getSpentMoney(), wb.getAmount());

            int crossed = highestCrossed(ratio);          // ratio 이하 최대 단계, 없으면 0
            if (crossed == 0) {
                return;                                   // 아직 25%도 안 넘음
            }

            Integer maxSent = notificationDao.selectMaxThreshold(wb.getId());
            int already = (maxSent == null) ? 0 : maxSent;
            if (crossed <= already) {
                return;                                   // 이미 그 단계까지 보냄 (중복 방지)
            }

            Notification n = new Notification();
            n.setUserId(userId);
            n.setWeeklyBudgetId(wb.getId());
            n.setThreshold(crossed);
            n.setCurrentBudget(wb.getAmount());           // 스냅샷: 그 시점 예산
            n.setSpentMoney(wb.getSpentMoney());          // 스냅샷: 그 시점 지출합
            n.setRatio(ratio);
            notificationDao.insertNotification(n);

        } catch (DuplicateKeyException e) {
            // UNIQUE(weekly_budget_id, threshold) 안전망 작동 = 동시성으로 이미 같은 단계 INSERT됨.
            // 정상 상황이므로 무시.
        } catch (Exception e) {
            // 알림 실패가 지출 등록을 망치면 안 됨 → 삼키고 로깅만.
            log.warn("알림 평가 실패 userId={} date={}", userId, date, e);
        }
    }

    // 예산 수정 시: 그 주 알림 전부 삭제(기준 리셋) -> 새 금액 기준 재평가 (DEC-0017)
    // @Transactional 없음: 예산 수정 트랜잭션에 참여하되, 알림 실패가 예산 수정을 롤백시키지 않게 try-catch로 삼킴
    @Override
    public void reevaluateOnBudgetChange(Long userId, Long weeklyBudgetId, LocalDate weekDate) {
        try {
            notificationDao.deleteByWeeklyBudgetId(weeklyBudgetId);   // 이전 단계 기록 리셋
            evaluateExpense(userId, weekDate);                        // 새 기준의 최고 1건 재평가
        } catch (Exception e) {
            log.warn("예산 수정 알림 재평가 실패 userId={} weeklyBudgetId={}", userId, weeklyBudgetId, e);
        }
    }

    // ratio 이하 최대 임계 단계 반환 (어느 단계도 못 넘으면 0)
    private int highestCrossed(BigDecimal ratio) {
        int crossed = 0;
        for (int t : THRESHOLDS) {
            if (ratio.compareTo(BigDecimal.valueOf(t)) >= 0) {
                crossed = t;                              // 넘은 단계마다 갱신 -> 마지막 값이 최고 단계
            }
        }
        return crossed;
    }

    // 사용률 % (spent/amount×100, 2자리 반올림, 0 가드) - budget calcRatio와 동일
    private BigDecimal calcRatio(BigDecimal spent, BigDecimal amount) {
        if (amount == null || amount.signum() == 0 || spent == null) {
            return BigDecimal.ZERO;
        }
        return spent.multiply(BigDecimal.valueOf(100))
                    .divide(amount, 2, RoundingMode.HALF_UP);
    }
}