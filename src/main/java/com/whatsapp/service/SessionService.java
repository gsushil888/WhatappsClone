package com.whatsapp.service;

import com.whatsapp.entity.User;
import com.whatsapp.entity.UserSession;
import com.whatsapp.repository.UserSessionRepository;
import com.whatsapp.util.JwtUtil;
import com.whatsapp.util.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

	private final UserSessionRepository userSessionRepository;
	private final JwtUtil jwtUtil;
	
	@Value("${jwt.refresh-token-validity:86400000}")
	private long refreshTokenExpiration;

	@Autowired(required = false)
	private RedisTemplate<String, Object> redisTemplate;

	private static final String SESSION_CACHE_KEY = "session:";
	private static final long CACHE_TTL_MINUTES = 30;

	public boolean isTokenExpired(String token) {
		return jwtUtil.isTokenExpired(token);
	}

	public Optional<UserSession> findActiveSessionByDevice(Long userId, String deviceFingerprint) {
		String cacheKey = SESSION_CACHE_KEY + userId + ":" + deviceFingerprint;

		return Optional.ofNullable(redisTemplate)
				.flatMap(redis -> Optional.ofNullable((UserSession) redis.opsForValue().get(cacheKey))
						.filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now())).map(session -> {
							log.debug("[SERVICE] [CACHE-HIT] Session found - userId: {}", userId);
							return session;
						}))
				.or(() -> {
					log.debug("[SERVICE] [DB-CALL] findByUserIdAndDeviceFingerprintAndStatusAndExpiresAtAfter - userId: {}", userId);
					Optional<UserSession> session = userSessionRepository
							.findByUserIdAndDeviceFingerprintAndStatusAndExpiresAtAfter(userId, deviceFingerprint,
									UserSession.SessionStatus.ACTIVE, LocalDateTime.now());

					session.ifPresent(s -> Optional.ofNullable(redisTemplate).ifPresent(
							redis -> redis.opsForValue().set(cacheKey, s, CACHE_TTL_MINUTES, TimeUnit.MINUTES)));

					return session;
				});
	}

	public List<UserSession> findActiveSessionsByUser(Long userId) {
		log.debug("[SERVICE] [DB-CALL] findActiveSessionsByUserId - userId: {}", userId);
		return userSessionRepository.findActiveSessionsByUserId(userId, UserSession.SessionStatus.ACTIVE,
				LocalDateTime.now());
	}

	public UserSession createSession(User user) {
		String deviceFingerprint = getDeviceFingerprintFromHeaders();
		String ipAddress = RequestContext.getIpAddress();
		String userAgent = RequestContext.getUserAgent();

		log.info("[SERVICE] Creating session - userId: {}, device: {}, IP: {}", user.getId(), deviceFingerprint,
				ipAddress);

		Optional<UserSession> existingSession = findActiveSessionByDevice(user.getId(), deviceFingerprint);
		if (existingSession.isPresent()) {
			log.info("[SERVICE] Returning existing session: {}", existingSession.get().getId());
			return existingSession.get();
		}

		log.info("[SERVICE] Creating new session - userId: {}", user.getId());
		log.debug("[SERVICE] [DB-CALL] save(UserSession) - userId: {}", user.getId());
		LocalDateTime now = LocalDateTime.now();
		long sessionExpirySeconds = refreshTokenExpiration / 1000;
		UserSession session = UserSession.builder().id(UUID.randomUUID().toString()).user(user)
				.type(determineSessionType(userAgent)).status(UserSession.SessionStatus.ACTIVE)
				.deviceFingerprint(deviceFingerprint).deviceType(RequestContext.getDeviceType())
				.deviceOs(RequestContext.getDeviceOs()).deviceBrowser(RequestContext.getDeviceBrowser())
				.deviceModel(RequestContext.getDeviceModel()).ipAddress(ipAddress).userAgent(userAgent)
				.expiresAt(now.plusSeconds(sessionExpirySeconds)).lastActivityAt(now).createdAt(now).build();

		UserSession savedSession = userSessionRepository.save(session);
		log.info("[SERVICE] Session created - sessionId: {}, userId: {}", savedSession.getId(), user.getId());
		return savedSession;
	}

	private String getDeviceFingerprintFromHeaders() {
		return Stream
				.of(Optional.ofNullable(RequestContext.getHeaders()).map(h -> h.get("x_device_fingerprint")),
						Optional.ofNullable(RequestContext.getDeviceInfo()).map(d -> d.get("device_fingerprint")))
				.flatMap(Optional::stream).filter(fp -> fp != null && !fp.trim().isEmpty()).findFirst()
				.orElseGet(() -> {
					String fallback = "fp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
					log.warn("[FALLBACK] Generated device fingerprint: {}", fallback);
					return fallback;
				});
	}

	@Transactional
	public void updateSessionTokens(UserSession session, String accessToken, String refreshToken) {
		LocalDateTime now = LocalDateTime.now();
		long sessionExpirySeconds = refreshTokenExpiration / 1000;
		LocalDateTime newExpiresAt = now.plusSeconds(sessionExpirySeconds);
		log.debug("[SERVICE] [DB-CALL] updateSessionTokens - sessionId: {}", session.getId());

		session.setJwtToken(accessToken);
		session.setRefreshToken(refreshToken);
		session.setLastActivityAt(now);
		session.setExpiresAt(newExpiresAt);
		session.setIpAddress(RequestContext.getIpAddress());
		session.setUserAgent(RequestContext.getUserAgent());
		userSessionRepository.save(session);

		Optional.ofNullable(redisTemplate).ifPresent(redis -> {
			String cacheKey = SESSION_CACHE_KEY + session.getUser().getId() + ":" + session.getDeviceFingerprint();
			redis.delete(cacheKey);
		});
	}

	@Transactional
	public void updateSessionAndUserPresence(UserSession session, User user) {
		LocalDateTime now = LocalDateTime.now();
		log.debug("[SERVICE] [DB-CALL] updateSessionTokens + updateUserPresence - userId: {}, sessionId: {}", user.getId(),
				session.getId());

		userSessionRepository.updateSessionTokens(session.getId(), session.getJwtToken(), session.getRefreshToken(),
				now, RequestContext.getIpAddress(), RequestContext.getUserAgent());

		userSessionRepository.updateUserPresence(user.getId(), true, now, now);

		Optional.ofNullable(redisTemplate).ifPresent(redis -> {
			String cacheKey = SESSION_CACHE_KEY + user.getId() + ":" + session.getDeviceFingerprint();
			redis.delete(cacheKey);
		});
	}

	@Transactional
	public void revokeAllUserSessions(Long userId) {
		log.debug("[SERVICE] [DB-CALL] findByUserIdAndStatus + save(UserSession) - userId: {}", userId);
		userSessionRepository.findByUserIdAndStatus(userId, UserSession.SessionStatus.ACTIVE).stream()
				.peek(session -> session.setStatus(UserSession.SessionStatus.REVOKED))
				.forEach(userSessionRepository::save);

		Optional.ofNullable(redisTemplate).ifPresent(redis -> {
			String pattern = SESSION_CACHE_KEY + userId + ":*";
			redis.keys(pattern).forEach(redis::delete);
		});
	}

	@Transactional
	public void revokeSession(String sessionId) {
		log.debug("[SERVICE] [DB-CALL] findById + save(UserSession) - sessionId: {}", sessionId);
		userSessionRepository.findById(sessionId).ifPresent(session -> {
			session.setStatus(UserSession.SessionStatus.REVOKED);
			userSessionRepository.save(session);

			Optional.ofNullable(redisTemplate).ifPresent(redis -> {
				String cacheKey = SESSION_CACHE_KEY + session.getUser().getId() + ":" + session.getDeviceFingerprint();
				redis.delete(cacheKey);
			});
		});
	}

	private UserSession.SessionType determineSessionType(String userAgent) {
		return Optional.ofNullable(userAgent).map(String::toLowerCase)
				.flatMap(ua -> Stream
						.of(Map.entry(UserSession.SessionType.MOBILE, List.of("mobile", "android", "iphone")),
								Map.entry(UserSession.SessionType.TABLET, List.of("tablet", "ipad")),
								Map.entry(UserSession.SessionType.DESKTOP, List.of("electron", "desktop")))
						.filter(entry -> entry.getValue().stream().anyMatch(ua::contains)).map(Map.Entry::getKey)
						.findFirst())
				.orElse(UserSession.SessionType.WEB);
	}
}
