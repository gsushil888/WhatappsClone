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
@Table(name = "user_sessions",
    indexes = {@Index(name = "idx_session_user", columnList = "userId"),
        @Index(name = "idx_session_status", columnList = "status"),
        @Index(name = "idx_session_device", columnList = "deviceFingerprint"),
        @Index(name = "idx_session_expires", columnList = "expiresAt")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

  @Id
  @Column(name = "id", length = 100)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "type")
  private SessionType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private SessionStatus status;

  @Column(name = "device_fingerprint", length = 255)
  private String deviceFingerprint;

  // Device info as separate columns instead of JSON
  @Column(name = "device_type", length = 50)
  private String deviceType;

  @Column(name = "device_os", length = 100)
  private String deviceOs;

  @Column(name = "device_browser", length = 100)
  private String deviceBrowser;

  @Column(name = "device_model", length = 100)
  private String deviceModel;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "user_agent", length = 1000)
  private String userAgent;

  @Column(name = "jwt_token", length = 500)
  private String jwtToken;

  @Column(name = "refresh_token", length = 500)
  private String refreshToken;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "last_activity_at")
  private LocalDateTime lastActivityAt;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public enum SessionType {
    WEB, MOBILE, DESKTOP, TABLET
  }
  public enum SessionStatus {
    ACTIVE, EXPIRED, REVOKED, TEMP
  }
}
