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

       @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId " +
                     "AND s.deviceFingerprint = :deviceFingerprint " +
                     "AND s.status = :status " +
                     "AND s.expiresAt > :now")
       Optional<UserSession> findByUserIdAndDeviceFingerprintAndStatusAndExpiresAtAfter(
                     @Param("userId") Long userId,
                     @Param("deviceFingerprint") String deviceFingerprint,
                     @Param("status") UserSession.SessionStatus status,
                     @Param("now") LocalDateTime now);

       @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId " +
                     "AND s.status = :status " +
                     "AND s.expiresAt > :now")
       List<UserSession> findActiveSessionsByUserId(@Param("userId") Long userId,
                     @Param("status") UserSession.SessionStatus status,
                     @Param("now") LocalDateTime now);

       @Query("SELECT s FROM UserSession s WHERE s.user.id = :userId " +
                     "AND s.status = :status")
       List<UserSession> findByUserIdAndStatus(@Param("userId") Long userId,
                     @Param("status") UserSession.SessionStatus status);

       @Modifying
       @Query("UPDATE UserSession s SET s.status = :status, s.updatedAt = :now " +
                     "WHERE s.user.id = :userId AND s.status = :activeStatus")
       void invalidateAllUserSessions(@Param("userId") Long userId,
                     @Param("status") UserSession.SessionStatus status,
                     @Param("activeStatus") UserSession.SessionStatus activeStatus,
                     @Param("now") LocalDateTime now);

       @Modifying
       @Query("UPDATE UserSession s SET s.lastActivityAt = :now, s.updatedAt = :now " +
                     "WHERE s.id = :sessionId")
       void updateLastActivity(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

       @Modifying
       @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
       void deleteExpiredSessions(@Param("now") LocalDateTime now);
}