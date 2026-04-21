package com.whatsapp.service;

import com.whatsapp.entity.ErrorCode;
import com.whatsapp.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

  private final RedisTemplate<String, String> redisTemplate;

  // Fallback in-memory cache
  private final Map<String, int[]> loginAttempts = new ConcurrentHashMap<>();

  private static final int MAX_ATTEMPTS = 5;
  private static final long COOLDOWN_MINUTES = 2;
  private static final long COOLDOWN_MILLIS = COOLDOWN_MINUTES * 60 * 1000;

  public void checkLoginRateLimit(Long userId, String deviceFingerprint) {
    String key = "login_attempts:" + userId + ":" + deviceFingerprint;

    try {
      checkRateLimitRedis(key);
    } catch (AuthException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Redis unavailable, using in-memory rate limiting: {}", e.getMessage());
      checkRateLimitInMemory(userId, deviceFingerprint);
    }
  }

  private void checkRateLimitRedis(String key) {
    log.info("Checking rate limit using redis for key =>" + key);
    String attemptsStr = redisTemplate.opsForValue().get(key);
    int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

    if (attempts >= MAX_ATTEMPTS) {
      Long ttl = redisTemplate.getExpire(key);
      if (ttl != null && ttl > 0) {
        throw new AuthException(ErrorCode.SYS_RATE_LIMIT_EXCEEDED,
            "Too many login attempts. Try again in " + ttl + " seconds");
      }
    }

    // Increment attempts with expiry
    redisTemplate.opsForValue().set(key, String.valueOf(attempts + 1),
        Duration.ofMinutes(COOLDOWN_MINUTES));
  }

  private void checkRateLimitInMemory(Long userId, String deviceFingerprint) {
    String key = userId + ":" + deviceFingerprint;
    long currentTime = System.currentTimeMillis();

    int[] attempts = loginAttempts.computeIfAbsent(key, k -> new int[] {0, 0});
    long lastAttemptTime = attempts[1];

    // Reset attempts if cooldown period has passed
    if (currentTime - lastAttemptTime > COOLDOWN_MILLIS) {
      attempts[0] = 0;
    }

    // Check if rate limited
    if (attempts[0] >= MAX_ATTEMPTS) {
      long timeLeft = COOLDOWN_MILLIS - (currentTime - lastAttemptTime);
      if (timeLeft > 0) {
        throw new AuthException(ErrorCode.SYS_RATE_LIMIT_EXCEEDED,
            "Too many login attempts. Try again in " + (timeLeft / 1000) + " seconds");
      }
    }

    // Increment attempts
    attempts[0]++;
    attempts[1] = (int) currentTime;
  }
}
