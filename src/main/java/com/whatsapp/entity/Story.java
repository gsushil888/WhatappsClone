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
@Table(name = "stories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Story {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  private StoryType type;

  private String content;
  private String mediaUrl;
  private String thumbnailUrl;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private StoryPrivacy privacy = StoryPrivacy.CONTACTS;

  @Builder.Default
  private Integer viewCount = 0;

  @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<StoryView> views;

  private LocalDateTime expiresAt;

  @Builder.Default
  private Boolean isActive = true;

  @CreationTimestamp
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  public enum StoryType {
    TEXT, IMAGE, VIDEO
  }
  public enum StoryPrivacy {
    PUBLIC, CONTACTS, CLOSE_FRIENDS, CUSTOM
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }
}
