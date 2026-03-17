package com.whatsapp.controller;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    @GetMapping("/status/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserPresence(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long requesterId) {
        
        Map<String, Object> presence = presenceService.getPresenceInfo(userId, requesterId);
        return ResponseEntity.ok(ApiResponse.success(presence));
    }

    @GetMapping("/online-users")
    public ResponseEntity<ApiResponse<List<Long>>> getOnlineUsers(
            @RequestHeader("X-User-Id") Long userId) {
        
        List<Long> onlineUsers = presenceService.getOnlineUsers();
        return ResponseEntity.ok(ApiResponse.success(onlineUsers));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<String>> updatePresence(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String status,
            @RequestParam(required = false) String deviceInfo) {
        
        if ("ONLINE".equals(status)) {
            presenceService.setUserOnline(userId, deviceInfo);
        } else {
            presenceService.setUserOffline(userId);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Presence updated successfully"));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<String>> heartbeat(
            @RequestHeader("X-User-Id") Long userId) {
        
        presenceService.updateLastSeen(userId);
        return ResponseEntity.ok(ApiResponse.success("Heartbeat received"));
    }
}