package com.whatsapp.service;

import com.whatsapp.entity.ErrorCode;
import com.whatsapp.entity.OtpVerification;
import com.whatsapp.exception.AuthException;
import com.whatsapp.repository.OtpVerificationRepository;
import com.whatsapp.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String generateOtp(String tempSessionId, String contactInfo, OtpVerification.OtpType otpType) {
        try {
            String otpCode = generateSecureOtp();
            String deviceFingerprint = RequestContext.getDeviceFingerprint();
            
            // Don't delete existing OTPs - allow multiple active OTPs for different devices
            // Only invalidate OTPs for the same device
            otpRepository.invalidateOtpsByDevice(contactInfo, otpType, deviceFingerprint, 
                    OtpVerification.OtpStatus.EXPIRED, OtpVerification.OtpStatus.PENDING);

            OtpVerification otp = OtpVerification.builder()
                    .tempSessionId(tempSessionId)
                    .contactInfo(contactInfo)
                    .otpCode(otpCode)
                    .otpType(otpType)
                    .otpStatus(OtpVerification.OtpStatus.PENDING)
                    .deviceFingerprint(deviceFingerprint)
                    .ipAddress(RequestContext.getIpAddress())
                    .expiresAt(LocalDateTime.now().plusMinutes(1))
                    .attemptsCount(0)
                    .build();

            otpRepository.save(otp);
            log.info("OTP generated for session: {} with contact: {} and device: {}", 
                    tempSessionId, contactInfo, deviceFingerprint);
            return otpCode;

        } catch (Exception e) {
            log.error("Failed to generate OTP: {}", e.getMessage());
            throw new AuthException(ErrorCode.AUTH_OTP_INVALID, "Failed to generate OTP");
        }
    }

    @Transactional
    public OtpVerification verifyOtp(String tempSessionId, String otpCode) {
        try {
            Optional<OtpVerification> otpOpt = otpRepository.findByTempSessionIdAndOtpStatusAndExpiresAtAfter(
                    tempSessionId, OtpVerification.OtpStatus.PENDING, LocalDateTime.now());
            
            if (otpOpt.isEmpty()) {
                OtpVerification expiredOtp = otpRepository.findByTempSessionId(tempSessionId)
                        .orElseThrow(() -> new AuthException(ErrorCode.AUTH_OTP_INVALID));
                throw new AuthException(ErrorCode.AUTH_OTP_EXPIRED, "OTP expired. Please request a new one.");
            }
            
            OtpVerification otp = otpOpt.get();

            if (!otp.canAttempt()) {
                otp.setOtpStatus(OtpVerification.OtpStatus.FAILED);
                otpRepository.save(otp);
                throw new AuthException(ErrorCode.AUTH_OTP_ATTEMPTS_EXCEEDED);
            }

            otp.setAttemptsCount(otp.getAttemptsCount() + 1);

            if (!otp.getOtpCode().equals(otpCode)) {
                otpRepository.save(otp);
                throw new AuthException(ErrorCode.AUTH_OTP_INVALID);
            }

            otp.setOtpStatus(OtpVerification.OtpStatus.VERIFIED);
            otp.setVerifiedAt(LocalDateTime.now());
            otpRepository.save(otp);

            log.info("OTP validated successfully for session: {}", tempSessionId);
            return otp;

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("OTP validation failed: {}", e.getMessage());
            throw new AuthException(ErrorCode.AUTH_OTP_INVALID, "OTP validation failed");
        }
    }

    @Transactional(readOnly = true)
    public Optional<OtpVerification> findActiveOtp(String contactInfo, OtpVerification.OtpType otpType) {
        return otpRepository.findByContactInfoAndOtpTypeAndOtpStatusAndExpiresAtAfter(
                contactInfo, otpType, OtpVerification.OtpStatus.PENDING, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<OtpVerification> findActiveOtpByDevice(String contactInfo, OtpVerification.OtpType otpType, String deviceFingerprint) {
        return otpRepository.findByContactInfoAndOtpTypeAndDeviceFingerprintAndOtpStatusAndExpiresAtAfter(
                contactInfo, otpType, deviceFingerprint, OtpVerification.OtpStatus.PENDING, LocalDateTime.now());
    }

    private String generateSecureOtp() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }
}