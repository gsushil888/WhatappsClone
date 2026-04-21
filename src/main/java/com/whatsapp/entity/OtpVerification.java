package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String tempSessionId;
  private String contactInfo;
  private String otpCode;

  @Enumerated(EnumType.STRING)
  private OtpType otpType;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private OtpStatus otpStatus = OtpStatus.PENDING;

  private String deviceFingerprint;
  private String ipAddress;
  private LocalDateTime expiresAt;
  private LocalDateTime verifiedAt;

  @Builder.Default
  private Integer attemptsCount = 0;

  @CreationTimestamp
  private LocalDateTime createdAt;

  public enum OtpType {
    EMAIL_LOGIN, PHONE_LOGIN, REGISTRATION, PASSWORD_RESET
  }

  public enum OtpStatus {
    PENDING, VERIFIED, EXPIRED, FAILED
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  public boolean canAttempt() {
    return attemptsCount < 3 && !isExpired() && otpStatus == OtpStatus.PENDING;
  }
}
