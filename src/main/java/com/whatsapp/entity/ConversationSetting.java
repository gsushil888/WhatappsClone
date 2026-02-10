package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_settings", indexes = {
    @Index(name = "idx_setting_conversation", columnList = "conversationId"),
    @Index(name = "idx_setting_key", columnList = "settingKey"),
    @Index(name = "idx_setting_active", columnList = "isActive")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(name = "setting_key", nullable = false)
    private SettingKey settingKey;

    @Column(name = "setting_value", length = 500)
    private String settingValue;

    @Column(name = "is_active")
    private boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_by_user_id")
    private User setByUser;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SettingKey {
        ONLY_ADMINS_ADD_MEMBERS,
        ONLY_ADMINS_EDIT_INFO,
        ONLY_ADMINS_SEND_MESSAGES,
        DISAPPEARING_MESSAGES_DURATION,
        ANNOUNCEMENT_ONLY,
        MUTE_NOTIFICATIONS,
        WALLPAPER_URL,
        ENCRYPTION_ENABLED,
        BACKUP_ENABLED,
        READ_RECEIPTS_ENABLED,
        TYPING_INDICATORS_ENABLED,
        MEDIA_AUTO_DOWNLOAD,
        LINK_PREVIEW_ENABLED
    }
}