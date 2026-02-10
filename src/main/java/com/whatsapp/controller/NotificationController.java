package com.whatsapp.controller;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.NotificationDto;
import com.whatsapp.service.NotificationService;
import com.whatsapp.util.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerDevice(
            Authentication authentication,
            @Valid @RequestBody NotificationDto.RegisterDeviceRequest request) {

        Long userId = (Long) authentication.getPrincipal();
        notificationService.registerDevice(userId, request);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .correlationId(RequestContext.getCorrelationId())
                .message("Device registered for push notifications")
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationDto.NotificationListResponse>> getNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {

        Long userId = (Long) authentication.getPrincipal();
        NotificationDto.NotificationListResponse response = notificationService.getNotifications(userId, limit, offset, unreadOnly);

        return ResponseEntity.ok(ApiResponse.<NotificationDto.NotificationListResponse>builder()
                .success(true)
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .build());
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId) {

        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAsRead(userId, notificationId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .correlationId(RequestContext.getCorrelationId())
                .message("Notification marked as read")
                .build());
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .correlationId(RequestContext.getCorrelationId())
                .message("All notifications marked as read")
                .build());
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            Authentication authentication,
            @PathVariable Long notificationId) {

        Long userId = (Long) authentication.getPrincipal();
        notificationService.deleteNotification(userId, notificationId);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .correlationId(RequestContext.getCorrelationId())
                .message("Notification deleted successfully")
                .build());
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationDto.NotificationSettingsResponse>> getNotificationSettings(
            Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        NotificationDto.NotificationSettingsResponse response = notificationService.getNotificationSettings(userId);

        return ResponseEntity.ok(ApiResponse.<NotificationDto.NotificationSettingsResponse>builder()
                .success(true)
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .build());
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationDto.NotificationSettingsResponse>> updateNotificationSettings(
            Authentication authentication,
            @Valid @RequestBody NotificationDto.UpdateNotificationSettingsRequest request) {

        Long userId = (Long) authentication.getPrincipal();
        NotificationDto.NotificationSettingsResponse response = notificationService.updateNotificationSettings(userId, request);

        return ResponseEntity.ok(ApiResponse.<NotificationDto.NotificationSettingsResponse>builder()
                .success(true)
                .data(response)
                .correlationId(RequestContext.getCorrelationId())
                .message("Notification settings updated successfully")
                .build());
    }
}