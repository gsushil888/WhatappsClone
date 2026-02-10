package com.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterDeviceRequest {
        @NotBlank(message = "Push token is required")
        private String pushToken;
        
        @NotBlank(message = "Platform is required")
        private String platform; // android, ios, web
        
        private String deviceFingerprint;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NotificationResponse {
        private Long id;
        private String type;
        private String title;
        private String message;
        private Object data;
        private Boolean isRead;
        private LocalDateTime createdAt;
        private LocalDateTime readAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NotificationListResponse {
        private List<NotificationResponse> notifications;
        private Integer unreadCount;
        private ApiResponse.PaginationInfo pagination;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NotificationSettingsResponse {
        private Boolean messageNotifications;
        private Boolean callNotifications;
        private Boolean groupNotifications;
        private Boolean soundEnabled;
        private Boolean vibrationEnabled;
        private String notificationTone;
        private Boolean showPreview;
        private String quietHoursStart;
        private String quietHoursEnd;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateNotificationSettingsRequest {
        private Boolean messageNotifications;
        private Boolean callNotifications;
        private Boolean groupNotifications;
        private Boolean soundEnabled;
        private Boolean vibrationEnabled;
        private String notificationTone;
        private Boolean showPreview;
        private String quietHoursStart;
        private String quietHoursEnd;
    }
}