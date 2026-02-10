package com.whatsapp.service;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.NotificationDto;
import com.whatsapp.entity.ErrorCode;
import com.whatsapp.entity.Notification;
import com.whatsapp.entity.User;
import com.whatsapp.exception.UserException;
import com.whatsapp.repository.NotificationRepository;
import com.whatsapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerDevice(Long userId, NotificationDto.RegisterDeviceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        
        log.info("Registering device for push notifications: userId={}, platform={}, token={}", 
                userId, request.getPlatform(), request.getPushToken().substring(0, 10) + "...");
        
        // Implementation for registering device for push notifications
        // This would typically involve saving the push token to database
        // and registering with push notification service (FCM, APNS, etc.)
    }

    @Transactional(readOnly = true)
    public NotificationDto.NotificationListResponse getNotifications(Long userId, int limit, int offset, boolean unreadOnly) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        List<Notification> notifications;
        
        if (unreadOnly) {
            notifications = notificationRepository.findUnreadByUserId(userId, pageable);
        } else {
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        
        List<NotificationDto.NotificationResponse> notificationResponses = notifications.stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());
        
        long unreadCount = notificationRepository.countUnreadByUserId(userId);
        long totalCount = notificationRepository.countByUserId(userId);
        
        return NotificationDto.NotificationListResponse.builder()
                .notifications(notificationResponses)
                .unreadCount((int) unreadCount)
                .pagination(ApiResponse.PaginationInfo.builder()
                        .page(offset / limit + 1)
                        .limit(limit)
                        .total((int) totalCount)
                        .hasNext(notificationResponses.size() == limit)
                        .build())
                .build();
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new UserException(ErrorCode.RESOURCE_NOT_FOUND));
        
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findUnreadByUserId(userId);
        
        unreadNotifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        });
        
        notificationRepository.saveAll(unreadNotifications);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new UserException(ErrorCode.RESOURCE_NOT_FOUND));
        
        notificationRepository.delete(notification);
    }

    @Transactional(readOnly = true)
    public NotificationDto.NotificationSettingsResponse getNotificationSettings(Long userId) {
        // Mock implementation - in real app, this would fetch from user preferences
        return NotificationDto.NotificationSettingsResponse.builder()
                .messageNotifications(true)
                .callNotifications(true)
                .groupNotifications(true)
                .soundEnabled(true)
                .vibrationEnabled(true)
                .showPreview(true)
                .notificationTone("default")
                .quietHoursStart("22:00")
                .quietHoursEnd("08:00")
                .build();
    }

    @Transactional
    public NotificationDto.NotificationSettingsResponse updateNotificationSettings(Long userId, 
            NotificationDto.UpdateNotificationSettingsRequest request) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        
        // Implementation for updating notification settings
        log.info("Updating notification settings for user {}", userId);
        
        // Return updated settings
        return NotificationDto.NotificationSettingsResponse.builder()
                .messageNotifications(request.getMessageNotifications())
                .callNotifications(request.getCallNotifications())
                .groupNotifications(request.getGroupNotifications())
                .soundEnabled(request.getSoundEnabled())
                .vibrationEnabled(request.getVibrationEnabled())
                .showPreview(request.getShowPreview())
                .notificationTone(request.getNotificationTone())
                .quietHoursStart(request.getQuietHoursStart())
                .quietHoursEnd(request.getQuietHoursEnd())
                .build();
    }

    @Transactional
    public void sendNotification(Long userId, String type, String title, String message, Object data) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .data(data != null ? data.toString() : null)
                .isRead(false)
                .build();
        
        notificationRepository.save(notification);
        
        // Send push notification to user's devices
        sendPushNotification(userId, title, message, data);
    }

    private void sendPushNotification(Long userId, String title, String message, Object data) {
        // Implementation for sending push notifications
        // This would integrate with FCM, APNS, or other push notification services
        log.info("Sending push notification to user {}: {}", userId, title);
    }

    private NotificationDto.NotificationResponse mapToNotificationResponse(Notification notification) {
        return NotificationDto.NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getData())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}