package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_admin_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupAdminAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User adminUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Enumerated(EnumType.STRING)
    private AdminActionType actionType;

    private String actionDetails;
    private String reason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum AdminActionType {
        ADD_MEMBER,
        REMOVE_MEMBER,
        PROMOTE_TO_ADMIN,
        DEMOTE_FROM_ADMIN,
        UPDATE_GROUP_NAME,
        UPDATE_GROUP_DESCRIPTION,
        UPDATE_GROUP_IMAGE,
        UPDATE_GROUP_SETTINGS,
        RESTRICT_MEMBER,
        UNRESTRICT_MEMBER,
        BAN_MEMBER,
        UNBAN_MEMBER
    }
}