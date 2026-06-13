package com.bp.jaringochi.domain.notification.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bp.jaringochi.domain.notification.dto.Notification;
import com.bp.jaringochi.domain.notification.service.NotificationService;
import com.bp.jaringochi.exception.BusinessException;
import com.bp.jaringochi.exception.ErrorCode;
import com.bp.jaringochi.global.response.Response;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 5-1. 목록 (isRead 생략 시 전체)
    @GetMapping
    public Response<List<Notification>> getNotifications(
            @RequestParam(required = false) Integer isRead,
            Authentication authentication) {
        List<Notification> list = notificationService.getNotifications(getCurrentUserId(authentication), isRead);
        return Response.success(list);
    }

    // 5-2. 안읽은 개수
    @GetMapping("/unread-count")
    public Response<Map<String, Integer>> getUnreadCount(Authentication authentication) {
        int count = notificationService.getUnreadCount(getCurrentUserId(authentication));
        return Response.success(Map.of("count", count));
    }

    // 5-3. 단건 읽음
    @PatchMapping("/{id}/read")
    public Response<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markAsRead(getCurrentUserId(authentication), id);
        return Response.success("읽음 처리되었습니다.");
    }

    // 5-4. 전체 읽음
    @PatchMapping("/read-all")
    public Response<Void> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(getCurrentUserId(authentication));
        return Response.success("모든 알림을 읽음 처리했습니다.");
    }

    // ===== 토큰에서 userId 추출 (거래 컨트롤러와 동일 패턴) =====
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException(ErrorCode.USER_UNAUTHORIZED);
        }
        return Long.valueOf(jwt.getSubject());
    }
}