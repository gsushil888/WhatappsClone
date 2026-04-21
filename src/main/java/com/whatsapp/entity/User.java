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
@Table(name = "users",
    indexes = {@Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_phone", columnList = "phoneNumber"),
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_status", columnList = "status"),
        @Index(name = "idx_user_online", columnList = "isOnline")})
@NamedEntityGraph(name = "User.basic", attributeNodes = {})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, length = 50)
  private String username;

  @Column(unique = true, length = 100)
  private String email;

  @Column(unique = true, length = 20)
  private String phoneNumber;

  private String password;

  private String firstName;

  private String lastName;

  @Column(length = 100)
  private String displayName;

  @Column(length = 500)
  private String profilePictureUrl;

  @Column(length = 500)
  private String aboutText;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private UserStatus status = UserStatus.ACTIVE;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private AccountType accountType = AccountType.PERSONAL;

  @Builder.Default
  private boolean isOnline = false;

  private LocalDateTime lastSeenAt;

  private LocalDateTime lastActiveAt;

  @Builder.Default
  private boolean isVerified = false;

  @Builder.Default
  private boolean emailVerified = false;

  @Builder.Default
  private boolean phoneVerified = false;

  @Builder.Default
  private boolean twoFactorEnabled = false;

  @ToString.Exclude
  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
      orphanRemoval = true)
  private PrivacySettings privacySettings;

  @ToString.Exclude
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<UserSession> sessions;

  @ToString.Exclude
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Contact> contacts;

  @ToString.Exclude
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ConversationParticipant> conversationParticipants;

  @ToString.Exclude
  @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Message> sentMessages;

  @ToString.Exclude
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Story> stories;

  @CreationTimestamp
  private LocalDateTime createdAt;

  @UpdateTimestamp
  private LocalDateTime updatedAt;

  public enum UserStatus {
    ACTIVE, INACTIVE, SUSPENDED, DELETED
  }
  public enum AccountType {
    PERSONAL, BUSINESS
  }
}
