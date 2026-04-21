package com.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AuthDto {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LoginRequest {
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid mobile number format")
    private String mobile;

    private String username;

    private String password;

    public String getIdentifier() {
      if (email != null && !email.trim().isEmpty())
        return email;
      if (mobile != null && !mobile.trim().isEmpty())
        return mobile;
      if (username != null && !username.trim().isEmpty())
        return username;
      return null;
    }

    public String getIdentifierType() {
      String identifier = getIdentifier();
      if (identifier == null)
        return null;
      if (identifier.contains("@"))
        return "EMAIL";
      if (identifier.matches("^\\+?[1-9]\\d{1,14}$"))
        return "PHONE";
      return "USERNAME";
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class LoginResponse {
    private Boolean requiresOtp;
    private String tempSessionId;
    private String otpSentTo;
    private Integer expiresIn;
    private LocalDateTime expiresAt;
    private String message;

    // For direct login (username + password)
    private UserInfo user;
    private SessionInfo session;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserInfo {
    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String displayName;
    private String profilePictureUrl;
    private String aboutText;
    private Boolean isOnline;
    private LocalDateTime lastSeenAt;
    private String accountType;
    private Boolean isVerified;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SessionInfo {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private LocalDateTime expiresAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VerifyOtpRequest {
    @NotBlank(message = "Temp session ID is required")
    private String tempSessionId;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    private String otpCode;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RefreshTokenResponse {
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private LocalDateTime expiresAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LogoutRequest {
    @Builder.Default
    private Boolean logoutAllDevices = false;

    private String sessionId; // Optional: specific session to logout
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RegisterRequest {
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String userName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Size(max = 100, message = "Display name cannot exceed 100 characters")
    private String displayName;

    @Size(max = 500, message = "About text cannot exceed 500 characters")
    private String aboutText;

    private String firstName;

    private String lastName;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RegisterResponse {
    private String tempSessionId;
    private String otpSentTo;
    private Integer expiresIn;
    private LocalDateTime expiresAt;
    private Integer resendAvailableIn;
    private String username;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ResendOtpRequest {
    @NotBlank(message = "Temp session ID is required")
    private String tempSessionId;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SessionValidationResponse {
    private boolean valid;
    private Long userId;
    private LocalDateTime expiresAt;
    private String deviceType;
    private String message;
  }
}
