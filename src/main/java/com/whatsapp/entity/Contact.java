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
@Table(name = "contacts",
    indexes = {@Index(name = "idx_contact_user", columnList = "userId"),
        @Index(name = "idx_contact_target", columnList = "contactUserId"),
        @Index(name = "idx_contact_status", columnList = "status")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contact_user_id", nullable = false)
  private User contactUser;

  @Column(name = "display_name", length = 100)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private ContactStatus status;

  @Column(name = "is_blocked")
  private boolean isBlocked;

  @Column(name = "is_favorite")
  private boolean isFavorite;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public enum ContactStatus {
    ACTIVE, BLOCKED, DELETED
  }
}
