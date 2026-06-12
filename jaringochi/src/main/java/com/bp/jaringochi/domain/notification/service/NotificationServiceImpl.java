package com.bp.jaringochi.domain.notification.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bp.jaringochi.domain.notification.dao.NotificationDao;
import com.bp.jaringochi.domain.notification.dto.Notification;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationDao notificationDao;

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
}