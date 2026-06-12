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
}