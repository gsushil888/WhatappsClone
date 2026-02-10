package com.whatsapp.service;

import com.whatsapp.entity.OtpVerification;
import com.whatsapp.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpCleanupScheduler {

    private final OtpVerificationRepository otpRepository;

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanupExpiredOtps() {
        try {
            LocalDateTime now = LocalDateTime.now();
            otpRepository.deleteExpiredOtpsExcept(now, OtpVerification.OtpStatus.VERIFIED);
            log.debug("Expired OTPs cleanup completed at {} (verified OTPs preserved)", now);
        } catch (Exception e) {
            log.error("Failed to cleanup expired OTPs: {}", e.getMessage());
        }
    }
}