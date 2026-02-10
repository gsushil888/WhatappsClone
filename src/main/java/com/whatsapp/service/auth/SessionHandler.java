package com.whatsapp.service.auth;

import com.whatsapp.dto.AuthDto;
import com.whatsapp.entity.User;
import com.whatsapp.entity.UserSession;
import com.whatsapp.repository.UserRepository;
import com.whatsapp.service.EmailService;
import com.whatsapp.service.SessionService;
import com.whatsapp.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SessionHandler {
    
    private final SessionService sessionService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public AuthDto.LoginResponse createDirectLoginResponse(User user, UserSession existingSession) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), existingSession.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), existingSession.getId());
        
        sessionService.updateSessionTokens(existingSession, accessToken, refreshToken);
        
        user.setOnline(true);
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);
        
        // No welcome email on re-login with existing session
        
        return AuthDto.LoginResponse.builder()
            .requiresOtp(false)
            .user(mapToUserInfo(user))
            .session(AuthDto.SessionInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build())
            .build();
    }
    
    public AuthDto.LoginResponse createLoginResponse(User user) {
        UserSession session = sessionService.createSession(user);
        String accessToken = jwtUtil.generateAccessToken(user.getId(), session.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), session.getId());
        
        sessionService.updateSessionTokens(session, accessToken, refreshToken);
        
        // Send welcome email on successful login
        emailService.sendWelcomeEmail(user.getEmail(), user.getDisplayName());
        
        return AuthDto.LoginResponse.builder()
            .requiresOtp(false)
            .user(mapToUserInfo(user))
            .session(AuthDto.SessionInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(3600)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build())
            .build();
    }
    
    private AuthDto.UserInfo mapToUserInfo(User user) {
        return AuthDto.UserInfo.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .phoneNumber(user.getPhoneNumber())
            .displayName(user.getDisplayName())
            .profilePictureUrl(user.getProfilePictureUrl())
            .aboutText(user.getAboutText())
            .isOnline(user.isOnline())
            .lastSeenAt(user.getLastSeenAt())
            .accountType(user.getAccountType().name())
            .isVerified(user.isVerified())
            .build();
    }
}