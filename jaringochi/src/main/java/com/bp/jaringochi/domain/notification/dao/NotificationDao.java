package com.bp.jaringochi.domain.notification.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.bp.jaringochi.domain.notification.dto.Notification;

@Mapper
public interface NotificationDao {

    // 5-1. 목록 (isRead null이면 전체)
    List<Notification> selectByUser(@Param("userId") Long userId,
                                    @Param("isRead") Integer isRead);

    // 5-2. 안읽은 개수
    int countUnread(Long userId);

    // 5-3. 단건 읽음 처리
    int markRead(Long id);

    // 5-4. 전체 읽음 처리
    int markAllRead(Long userId);

    // 보조: 단건 조회 (존재 404 + 소유권 403 확인용)
    Notification selectById(Long id);
    
    // ==== 트리거가 알림을 만들 수 있게 함 ==== //
    // 트리거: 알림 생성 (id 자동 채움)
    int insertNotification(Notification n);

    // 트리거: 이 주 예산에 이미 보낸 최고 임계치 (없으면 null)
    Integer selectMaxThreshold(Long weeklyBudgetId);

    // 예산 수정 시: 그 주 예산의 알림 전부 삭제 (기준 리셋용, DEC-0017)
    int deleteByWeeklyBudgetId(Long weeklyBudgetId);

    // ==== 이벤트 알림: 옷 뽑기 기회(DRAW) / 월 레포트(REPORT) ==== //
    // 뽑기 자격 주 중 아직 DRAW 알림 없는 weekly_budget_id 목록
    List<Long> selectEligibleDrawWeeks(Long userId);

    // DRAW 알림 생성
    int insertDrawNotification(@Param("userId") Long userId,
                               @Param("weeklyBudgetId") Long weeklyBudgetId);

    // 이 (유저, 연, 월)로 이미 REPORT 알림이 있는지 (0/1+)
    int existsReportNotification(@Param("userId") Long userId,
                                 @Param("year") int year,
                                 @Param("month") int month);

    // REPORT 알림 생성
    int insertReportNotification(@Param("userId") Long userId,
                                 @Param("year") int year,
                                 @Param("month") int month);

    // REPORT 가드: 기준일(이번 달 시작) 이전에 가입했는지 (0/1)
    int countMemberBefore(@Param("userId") Long userId,
                          @Param("date") java.time.LocalDate date);
}