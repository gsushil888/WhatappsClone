package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "messages",
    indexes = {@Index(name = "idx_message_conversation", columnList = "conversationId"),
        @Index(name = "idx_message_sender", columnList = "senderId"),
        @Index(name = "idx_message_created_at", columnList = "createdAt"),
        @Index(name = "idx_message_type", columnList = "type"),
        @Index(name = "idx_message_status", columnList = "status")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conversation_id", nullable = false)
  private Conversation conversation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  private MessageType type;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<MessageAttachment> attachments;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reply_to_message_id")
  private Message replyToMessage;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "forwarded_from_message_id")
  private Message forwardedFromMessage;

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<MessageStatus> messageStatuses;

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<MessageReaction> reactions;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private DeliveryStatus status;

  @Column(name = "is_edited")
  @Builder.Default
  private boolean isEdited = false;

  @Column(name = "is_deleted")
  @Builder.Default
  private boolean isDeleted = false;

  @Column(name = "is_starred")
  @Builder.Default
  private boolean isStarred = false;

  @Column(name = "deleted_for_sender")
  @Builder.Default
  private boolean deletedForSender = false;

  // @Column(name = "media_url")
  // private String mediaUrl;

  @Column(name = "edited_at")
  private LocalDateTime editedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public enum MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION, CONTACT, STICKER
  }
  public enum DeliveryStatus {
    SENT, DELIVERED, READ
  }
}
