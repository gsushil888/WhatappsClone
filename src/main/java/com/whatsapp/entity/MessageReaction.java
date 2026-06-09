package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_reactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "message_id", nullable = false)
  private Message message;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // null = reaction on whole message; non-null = reaction on a specific attachment
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "attachment_id")
  private MessageAttachment attachment;

  @Column(length = 10, columnDefinition = "varchar(10) CHARACTER SET utf8mb4")
  private String emoji;

  @CreationTimestamp
  private LocalDateTime createdAt;
}
