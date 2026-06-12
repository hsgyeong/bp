package com.bp.jaringochi.domain.notification.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.notification.dto.Notification;
import com.bp.jaringochi.domain.notification.service.NotificationService;
import com.bp.jaringochi.global.response.Response;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 5-1. 목록 (isRead 생략 시 전체)
    @GetMapping
    public Response<List<Notification>> getNotifications(
            @RequestParam(required = false) Integer isRead) {
        List<Notification> list = notificationService.getNotifications(getCurrentUserId(), isRead);
        return Response.success(list);
    }

    // 5-2. 안읽은 개수
    @GetMapping("/unread-count")
    public Response<Map<String, Integer>> getUnreadCount() {
        int count = notificationService.getUnreadCount(getCurrentUserId());
        return Response.success(Map.of("count", count));
    }

    // 5-3. 단건 읽음
    @PatchMapping("/{id}/read")
    public Response<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(getCurrentUserId(), id);
        return Response.success("읽음 처리되었습니다.");
    }

    // 5-4. 전체 읽음
    @PatchMapping("/read-all")
    public Response<Void> markAllAsRead() {
        notificationService.markAllAsRead(getCurrentUserId());
        return Response.success("모든 알림을 읽음 처리했습니다.");
    }

    // ===== 임시 인증 (추후 실제 로그인 사용자로 교체) =====
    private Long getCurrentUserId() {
        return 1L;
    }
}