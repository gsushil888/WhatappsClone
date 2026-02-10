package com.whatsapp.repository;

import com.whatsapp.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByTempSessionIdAndOtpStatusAndExpiresAtAfter(
            String tempSessionId, 
            OtpVerification.OtpStatus status, 
            LocalDateTime now);
    
    Optional<OtpVerification> findByTempSessionId(String tempSessionId);

    Optional<OtpVerification> findByContactInfoAndOtpTypeAndOtpStatusAndExpiresAtAfter(
            String contactInfo,
            OtpVerification.OtpType otpType,
            OtpVerification.OtpStatus status,
            LocalDateTime now);

    Optional<OtpVerification> findByContactInfoAndOtpTypeAndDeviceFingerprintAndOtpStatusAndExpiresAtAfter(
            String contactInfo,
            OtpVerification.OtpType otpType,
            String deviceFingerprint,
            OtpVerification.OtpStatus status,
            LocalDateTime now);

    @Query("SELECT o FROM OtpVerification o WHERE o.contactInfo = :contactInfo " +
           "AND o.otpType = :otpType " +
           "AND o.otpStatus = :status " +
           "AND o.expiresAt > :now " +
           "ORDER BY o.createdAt DESC")
    Optional<OtpVerification> findLatestByContactInfoAndTypeAndStatus(
            @Param("contactInfo") String contactInfo,
            @Param("otpType") OtpVerification.OtpType otpType,
            @Param("status") OtpVerification.OtpStatus status,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.otpStatus = :status " +
           "WHERE o.contactInfo = :contactInfo AND o.otpType = :otpType " +
           "AND o.otpStatus = :currentStatus")
    void invalidateExistingOtps(@Param("contactInfo") String contactInfo,
                               @Param("otpType") OtpVerification.OtpType otpType,
                               @Param("status") OtpVerification.OtpStatus status,
                               @Param("currentStatus") OtpVerification.OtpStatus currentStatus);

    @Modifying
    @Query("UPDATE OtpVerification o SET o.otpStatus = :status " +
           "WHERE o.contactInfo = :contactInfo AND o.otpType = :otpType " +
           "AND o.deviceFingerprint = :deviceFingerprint AND o.otpStatus = :currentStatus")
    void invalidateOtpsByDevice(@Param("contactInfo") String contactInfo,
                               @Param("otpType") OtpVerification.OtpType otpType,
                               @Param("deviceFingerprint") String deviceFingerprint,
                               @Param("status") OtpVerification.OtpStatus status,
                               @Param("currentStatus") OtpVerification.OtpStatus currentStatus);

    @Modifying
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :now AND o.otpStatus != :verifiedStatus")
    void deleteExpiredOtpsExcept(@Param("now") LocalDateTime now, @Param("verifiedStatus") OtpVerification.OtpStatus verifiedStatus);

    @Modifying
    void deleteByContactInfoAndOtpType(String contactInfo, OtpVerification.OtpType otpType);

    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.contactInfo = :contactInfo " +
           "AND o.otpType = :otpType " +
           "AND o.createdAt > :since")
    long countRecentOtpsByContactInfo(@Param("contactInfo") String contactInfo,
                                     @Param("otpType") OtpVerification.OtpType otpType,
                                     @Param("since") LocalDateTime since);
}