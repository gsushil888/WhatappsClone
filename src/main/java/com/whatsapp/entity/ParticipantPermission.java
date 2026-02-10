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
@Table(name = "participant_permissions", indexes = {
    @Index(name = "idx_permission_participant", columnList = "participantId"),
    @Index(name = "idx_permission_key", columnList = "permissionKey"),
    @Index(name = "idx_permission_granted", columnList = "isGranted")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private ConversationParticipant participant;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_key", nullable = false)
    private PermissionKey permissionKey;

    @Column(name = "is_granted")
    private boolean isGranted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_user_id")
    private User grantedByUser;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum PermissionKey {
        ADD_MEMBERS,
        REMOVE_MEMBERS,
        EDIT_GROUP_INFO,
        SEND_MESSAGES,
        DELETE_MESSAGES,
        PIN_MESSAGES,
        CHANGE_GROUP_SETTINGS,
        PROMOTE_MEMBERS,
        DEMOTE_MEMBERS,
        VIEW_ADMIN_ACTIONS,
        MANAGE_CALLS,
        SEND_MEDIA,
        SEND_DOCUMENTS,
        CREATE_POLLS
    }
}