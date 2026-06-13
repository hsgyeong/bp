package com.bp.jaringochi.domain.notification.service;

import java.time.LocalDate;
import java.util.List;

import com.bp.jaringochi.domain.notification.dto.Notification;

public interface NotificationService {

    // 5-1. 목록 (isRead null이면 전체)
    List<Notification> getNotifications(Long userId, Integer isRead);

    // 5-2. 안읽은 개수
    int getUnreadCount(Long userId);

    // 5-3. 단건 읽음
    void markAsRead(Long userId, Long id);

    // 5-4. 전체 읽음
    void markAllAsRead(Long userId);
    
    // 트리거: 지출 등록 시 임계치 평가 후 필요하면 알림 1건 생성 
    void evaluateExpense(Long userId, LocalDate date);
}