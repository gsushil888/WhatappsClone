package com.whatsapp.service.auth;

import com.whatsapp.dto.AuthDto;
import com.whatsapp.entity.OtpVerification;
import com.whatsapp.service.EmailService;
import com.whatsapp.service.OtpService;
import com.whatsapp.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OtpHandler {
    
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthDto.LoginResponse handleOtpLogin(String contactInfo, OtpVerification.OtpType otpType, String displayName) {
        String maskedContact = maskContact(contactInfo, otpType);
        String deviceFingerprint = RequestContext.getDeviceFingerprint();
        
        Optional<OtpVerification> existingOtp = otpService.findActiveOtpByDevice(contactInfo, otpType, deviceFingerprint);
        
        if (existingOtp.isPresent()) {
            OtpVerification otp = existingOtp.get();
            long timeLeft = java.time.Duration.between(LocalDateTime.now(), otp.getExpiresAt()).getSeconds();
            
            if (timeLeft > 0) {
                return buildOtpResponse(otp.getTempSessionId(), maskedContact, (int) timeLeft, otp.getExpiresAt(), 
                    "Please wait. OTP already sent to " + maskedContact);
            }
        }
        
        String tempSessionId = generateTempSessionId();
        String otpCode = otpService.generateOtp(tempSessionId, contactInfo, otpType);
        
        if (otpType == OtpVerification.OtpType.EMAIL_LOGIN) {
            emailService.sendOtpEmail(contactInfo, otpCode, displayName);
        }
        
        return buildOtpResponse(tempSessionId, maskedContact, 60, LocalDateTime.now().plusMinutes(1), 
            "OTP sent to " + maskedContact);
    }
    
    private AuthDto.LoginResponse buildOtpResponse(String tempSessionId, String maskedContact, int expiresIn, 
            LocalDateTime expiresAt, String message) {
        return AuthDto.LoginResponse.builder()
            .requiresOtp(true)
            .tempSessionId(tempSessionId)
            .otpSentTo(maskedContact)
            .expiresIn(expiresIn)
            .expiresAt(expiresAt)
            .message(message)
            .build();
    }
    
    private String maskContact(String contact, OtpVerification.OtpType otpType) {
        return otpType == OtpVerification.OtpType.EMAIL_LOGIN ? maskEmail(contact) : maskPhone(contact);
    }
    
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 2 ? email.substring(0, 2) + "***" + email.substring(atIndex) : "***" + email.substring(atIndex);
    }
    
    private String maskPhone(String phone) {
        return phone.length() > 4 ? phone.substring(0, 2) + "***" + phone.substring(phone.length() - 2) : "***";
    }
    
    private String generateTempSessionId() {
        return "temp_" + UUID.randomUUID().toString().replace("-", "");
    }
}