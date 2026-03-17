package com.whatsapp.service;

import com.whatsapp.entity.User;
import com.whatsapp.entity.UserPresence;
import com.whatsapp.repository.UserPresenceRepository;
import com.whatsapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final UserPresenceRepository presenceRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String PRESENCE_KEY = "presence:";
    private static final String ONLINE_USERS_KEY = "online_users";
    private static final String TYPING_KEY = "typing:";
    private static final long PRESENCE_TIMEOUT = 30; // seconds
    private static final long TYPING_TIMEOUT = 5; // seconds

    @Transactional
    public void setUserOnline(Long userId, String deviceInfo) {
        if (redisTemplate != null) {
            String key = PRESENCE_KEY + userId;
            Map<String, Object> presenceData = Map.of(
                "status", "ONLINE",
                "lastSeen", LocalDateTime.now().toString(),
                "deviceInfo", deviceInfo != null ? deviceInfo : ""
            );
            
            redisTemplate.opsForHash().putAll(key, presenceData);
            redisTemplate.expire(key, PRESENCE_TIMEOUT, TimeUnit.SECONDS);
            redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());
        }

        UserPresence presence = presenceRepository.findByUserId(userId)
            .orElse(UserPresence.builder()
                .userId(userId)
                .build());
                
        presence.setStatus(UserPresence.Status.ONLINE);
        presence.setLastOnline(LocalDateTime.now());
        presence.setDeviceInfo(deviceInfo);
        presenceRepository.save(presence);

        // Update User.lastActiveAt and isOnline for conversations
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActiveAt(LocalDateTime.now());
            user.setOnline(true);
            userRepository.save(user);
        });

        broadcastPresenceUpdate(userId, "ONLINE", LocalDateTime.now());
        log.info("User {} is now online", userId);
    }

    @Transactional
    public void setUserOffline(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        if (redisTemplate != null) {
            String key = PRESENCE_KEY + userId;
            
            Map<String, Object> presenceData = Map.of(
                "status", "OFFLINE",
                "lastSeen", now.toString()
            );
            
            redisTemplate.opsForHash().putAll(key, presenceData);
            redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());
        }

        presenceRepository.findByUserId(userId).ifPresent(presence -> {
            presence.setStatus(UserPresence.Status.OFFLINE);
            presence.setLastSeen(now);
            presenceRepository.save(presence);
        });

        // Update User.lastActiveAt and isOnline for conversations
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActiveAt(now);
            user.setOnline(false);
            userRepository.save(user);
        });

        broadcastPresenceUpdate(userId, "OFFLINE", now);
        log.info("User {} is now offline", userId);
    }

    public void updateLastSeen(Long userId) {
        if (redisTemplate != null) {
            String key = PRESENCE_KEY + userId;
            redisTemplate.opsForHash().put(key, "lastSeen", LocalDateTime.now().toString());
            redisTemplate.expire(key, PRESENCE_TIMEOUT, TimeUnit.SECONDS);
        }
    }

    public UserPresence.Status getUserStatus(Long userId) {
        if (redisTemplate != null) {
            String key = PRESENCE_KEY + userId;
            String status = (String) redisTemplate.opsForHash().get(key, "status");
            
            if (status != null) {
                return UserPresence.Status.valueOf(status);
            }
        }
        
        return presenceRepository.findByUserId(userId)
            .map(UserPresence::getStatus)
            .orElse(UserPresence.Status.OFFLINE);
    }

    public LocalDateTime getLastSeen(Long userId, Long requesterId) {
        if (!canViewLastSeen(userId, requesterId)) {
            return null;
        }
        
        if (redisTemplate != null) {
            String key = PRESENCE_KEY + userId;
            String lastSeenStr = (String) redisTemplate.opsForHash().get(key, "lastSeen");
            
            if (lastSeenStr != null) {
                return LocalDateTime.parse(lastSeenStr);
            }
        }
        
        return presenceRepository.findByUserId(userId)
            .map(UserPresence::getLastSeen)
            .orElse(null);
    }

    public List<Long> getOnlineUsers() {
        if (redisTemplate != null) {
            return redisTemplate.opsForSet().members(ONLINE_USERS_KEY)
                .stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .toList();
        }
        
        return presenceRepository.findOnlineUsers()
            .stream()
            .map(UserPresence::getUserId)
            .toList();
    }

    public Map<String, Object> getPresenceInfo(Long userId, Long requesterId) {
        UserPresence.Status status = getUserStatus(userId);
        LocalDateTime lastSeen = getLastSeen(userId, requesterId);
        
        return Map.of(
            "userId", userId,
            "status", status.name(),
            "lastSeen", lastSeen != null ? lastSeen.toString() : null,
            "isOnline", status == UserPresence.Status.ONLINE
        );
    }

    private void broadcastPresenceUpdate(Long userId, String status, LocalDateTime timestamp) {
        Map<String, Object> update = Map.of(
            "userId", userId,
            "status", status,
            "timestamp", timestamp.toString(),
            "lastSeen", timestamp.toString()
        );
        
        // Broadcast to all users who have this user in their contacts
        messagingTemplate.convertAndSend("/topic/presence", update);
    }

    private boolean canViewLastSeen(Long userId, Long requesterId) {
        if (userId.equals(requesterId)) {
            return true;
        }
        
        return presenceRepository.findByUserId(userId)
            .map(UserPresence::isShowLastSeen)
            .orElse(true);
    }

    public void cleanupOfflineUsers() {
        if (redisTemplate != null) {
            List<Long> onlineUsers = getOnlineUsers();
            for (Long userId : onlineUsers) {
                String key = PRESENCE_KEY + userId;
                if (!redisTemplate.hasKey(key)) {
                    setUserOffline(userId);
                }
            }
        }
    }

    public void setUserTyping(Long userId, Long conversationId) {
        if (redisTemplate != null) {
            String key = TYPING_KEY + conversationId + ":" + userId;
            redisTemplate.opsForValue().set(key, LocalDateTime.now().toString(), TYPING_TIMEOUT, TimeUnit.SECONDS);
        }
        
        Map<String, Object> typingUpdate = Map.of(
            "userId", userId,
            "conversationId", conversationId,
            "action", "start",
            "timestamp", System.currentTimeMillis()
        );
        
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId + "/typing", typingUpdate);
        log.debug("User {} started typing in conversation {}", userId, conversationId);
    }

    public void setUserStoppedTyping(Long userId, Long conversationId) {
        if (redisTemplate != null) {
            String key = TYPING_KEY + conversationId + ":" + userId;
            redisTemplate.delete(key);
        }
        
        Map<String, Object> typingUpdate = Map.of(
            "userId", userId,
            "conversationId", conversationId,
            "action", "stop",
            "timestamp", System.currentTimeMillis()
        );
        
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId + "/typing", typingUpdate);
        log.debug("User {} stopped typing in conversation {}", userId, conversationId);
    }
}