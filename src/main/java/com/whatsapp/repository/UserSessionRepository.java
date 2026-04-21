package com.whatsapp.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.whatsapp.entity.UserSession;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

  @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId "
      + "AND s.deviceFingerprint = :deviceFingerprint " + "AND s.status = :status "
      + "AND s.expiresAt > :now")
  Optional<UserSession> findByUserIdAndDeviceFingerprintAndStatusAndExpiresAtAfter(
      @Param("userId") Long userId, @Param("deviceFingerprint") String deviceFingerprint,
      @Param("status") UserSession.SessionStatus status, @Param("now") LocalDateTime now);

  @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId " + "AND s.status = :status "
      + "AND s.expiresAt > :now")
  List<UserSession> findActiveSessionsByUserId(@Param("userId") Long userId,
      @Param("status") UserSession.SessionStatus status, @Param("now") LocalDateTime now);

  @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId " + "AND s.status = :status")
  List<UserSession> findByUserIdAndStatus(@Param("userId") Long userId,
      @Param("status") UserSession.SessionStatus status);

  @Modifying
  @Query("UPDATE UserSession s SET s.status = :status, s.updatedAt = :now "
      + "WHERE s.user.id = :userId AND s.status = :activeStatus")
  void invalidateAllUserSessions(@Param("userId") Long userId,
      @Param("status") UserSession.SessionStatus status,
      @Param("activeStatus") UserSession.SessionStatus activeStatus,
      @Param("now") LocalDateTime now);

  @Modifying
  @Query("UPDATE UserSession s SET s.jwtToken = :accessToken, s.refreshToken = :refreshToken, "
      + "s.lastActivityAt = :now, s.ipAddress = :ipAddress, s.userAgent = :userAgent, s.updatedAt = :now "
      + "WHERE s.id = :sessionId")
  void updateSessionTokens(@Param("sessionId") String sessionId,
      @Param("accessToken") String accessToken, @Param("refreshToken") String refreshToken,
      @Param("now") LocalDateTime now, @Param("ipAddress") String ipAddress,
      @Param("userAgent") String userAgent);

  @Modifying
  @Query("UPDATE User u SET u.isOnline = :isOnline, u.lastSeenAt = :lastSeenAt, u.lastActiveAt = :lastActiveAt "
      + "WHERE u.id = :userId")
  void updateUserPresence(@Param("userId") Long userId, @Param("isOnline") boolean isOnline,
      @Param("lastSeenAt") LocalDateTime lastSeenAt,
      @Param("lastActiveAt") LocalDateTime lastActiveAt);

  @Modifying
  @Query("UPDATE UserSession s SET s.lastActivityAt = :now, s.updatedAt = :now "
      + "WHERE s.id = :sessionId")
  void updateLastActivity(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

  @Modifying
  @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
  void deleteExpiredSessions(@Param("now") LocalDateTime now);
}
