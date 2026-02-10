package com.whatsapp.service;

import com.whatsapp.entity.User;
import com.whatsapp.entity.UserSession;
import com.whatsapp.repository.UserSessionRepository;
import com.whatsapp.util.JwtUtil;
import com.whatsapp.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final JwtUtil jwtUtil;

    public Optional<UserSession> findActiveSessionByDevice(Long userId, String deviceFingerprint) {
        log.info("Looking for active session - userId: {}, deviceFingerprint: {}", userId, deviceFingerprint);
        Optional<UserSession> session = userSessionRepository.findByUserIdAndDeviceFingerprintAndStatusAndExpiresAtAfter(
                userId, deviceFingerprint, UserSession.SessionStatus.ACTIVE, LocalDateTime.now());
        log.info("Found active session: {}", session.isPresent());
        return session;
    }

    public List<UserSession> findActiveSessionsByUser(Long userId) {
        return userSessionRepository.findActiveSessionsByUserId(
                userId, UserSession.SessionStatus.ACTIVE, LocalDateTime.now());
    }

    public UserSession createSession(User user) {
        String deviceFingerprint = getDeviceFingerprintFromHeaders();
        String ipAddress = RequestContext.getIpAddress();
        String userAgent = RequestContext.getUserAgent();
        
        log.info("Creating session for user: {}, device: {}, IP: {}", user.getId(), deviceFingerprint, ipAddress);

        // Check if there's already an active session for this device
        Optional<UserSession> existingSession = findActiveSessionByDevice(user.getId(), deviceFingerprint);
        if (existingSession.isPresent()) {
            log.info("Returning existing session: {}", existingSession.get().getId());
            return existingSession.get();
        }

        log.info("Creating new session for user: {}", user.getId());
        LocalDateTime now = LocalDateTime.now();
        UserSession session = UserSession.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .type(determineSessionType(userAgent))
                .status(UserSession.SessionStatus.ACTIVE)
                .deviceFingerprint(deviceFingerprint)
                .deviceType(RequestContext.getDeviceType())
                .deviceOs(RequestContext.getDeviceOs())
                .deviceBrowser(RequestContext.getDeviceBrowser())
                .deviceModel(RequestContext.getDeviceModel())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(now.plusDays(30))
                .lastActivityAt(now)
                .createdAt(now)
                .build();

        UserSession savedSession = userSessionRepository.save(session);
        log.info("New session created with ID: {} for user: {}", savedSession.getId(), user.getId());
        return savedSession;
    }

    private String getDeviceFingerprintFromHeaders() {
        // Check headers first
        Map<String, String> headers = RequestContext.getHeaders();
        if (headers != null && headers.containsKey("x_device_fingerprint")) {
            String fingerprint = headers.get("x_device_fingerprint");
            if (fingerprint != null && !fingerprint.trim().isEmpty()) {
                log.info("Using header device fingerprint: {}", fingerprint);
                return fingerprint;
            }
        }
        
        // Check device info
        Map<String, String> deviceInfo = RequestContext.getDeviceInfo();
        if (deviceInfo != null && deviceInfo.containsKey("device_fingerprint")) {
            String fingerprint = deviceInfo.get("device_fingerprint");
            if (fingerprint != null && !fingerprint.trim().isEmpty()) {
                log.info("Using deviceInfo device fingerprint: {}", fingerprint);
                return fingerprint;
            }
        }
        
        String fallback = "fp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.warn("No device fingerprint found in headers or deviceInfo, using fallback: {}", fallback);
        log.warn("Headers available: {}", headers != null ? headers.keySet() : "null");
        log.warn("DeviceInfo available: {}", deviceInfo != null ? deviceInfo.keySet() : "null");
        return fallback;
    }

    public void updateSessionTokens(UserSession session, String accessToken, String refreshToken) {
        session.setJwtToken(accessToken);
        session.setRefreshToken(refreshToken);
        session.setLastActivityAt(LocalDateTime.now());
        session.setIpAddress(RequestContext.getIpAddress());
        session.setUserAgent(RequestContext.getUserAgent());
        userSessionRepository.save(session);
    }

    public void revokeAllUserSessions(Long userId) {
        userSessionRepository.findByUserIdAndStatus(userId, UserSession.SessionStatus.ACTIVE)
                .forEach(session -> {
                    session.setStatus(UserSession.SessionStatus.REVOKED);
                    userSessionRepository.save(session);
                });
    }

    public void revokeSession(String sessionId) {
        userSessionRepository.findById(sessionId)
                .ifPresent(session -> {
                    session.setStatus(UserSession.SessionStatus.REVOKED);
                    userSessionRepository.save(session);
                });
    }

    private UserSession.SessionType determineSessionType(String userAgent) {
        return Optional.ofNullable(userAgent).map(String::toLowerCase).map(ua -> {
            if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
                return UserSession.SessionType.MOBILE;
            } else if (ua.contains("tablet") || ua.contains("ipad")) {
                return UserSession.SessionType.TABLET;
            } else if (ua.contains("electron") || ua.contains("desktop")) {
                return UserSession.SessionType.DESKTOP;
            }
            return UserSession.SessionType.WEB;
        }).orElse(UserSession.SessionType.WEB);
    }
}