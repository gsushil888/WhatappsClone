package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conversation_participants",
    indexes = {@Index(name = "idx_participant_conversation", columnList = "conversationId"),
        @Index(name = "idx_participant_user", columnList = "userId"),
        @Index(name = "idx_participant_role", columnList = "role")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conversation_id", nullable = false)
  private Conversation conversation;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "role")
  private ParticipantRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private ParticipantStatus status;

  @Column(name = "joined_at")
  private LocalDateTime joinedAt;

  @Column(name = "left_at")
  private LocalDateTime leftAt;

  @Column(name = "last_read_at")
  private LocalDateTime lastReadAt;

  @Column(name = "last_read_message_id")
  private Long lastReadMessageId;

  @Column(name = "is_favorite", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  private Boolean isFavorite = false;

  @Column(name = "is_archived", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  private Boolean isArchived = false;

  @Column(name = "is_pinned", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  private Boolean isPinned = false;

  @Column(name = "is_muted", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  private Boolean isMuted = false;

  @Column(name = "mute_until")
  private LocalDateTime muteUntil;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "added_by")
  private User addedBy;

  @ToString.Exclude
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promoted_by")
  private User promotedBy;

  @Column(name = "promoted_at")
  private LocalDateTime promotedAt;

  @ToString.Exclude
  @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ParticipantPermission> permissions;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public enum ParticipantRole {
    MEMBER, ADMIN, OWNER
  }
  public enum ParticipantStatus {
    ACTIVE, LEFT, REMOVED, BANNED
  }
}
