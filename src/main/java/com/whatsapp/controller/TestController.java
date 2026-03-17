package com.whatsapp.controller;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class TestController {

    private final PresenceService presenceService;

    @PostMapping("/presence/online/{userId}")
    public ResponseEntity<ApiResponse<String>> testSetOnline(@PathVariable Long userId) {
        presenceService.setUserOnline(userId, "Test Device");
        return ResponseEntity.ok(ApiResponse.success("User " + userId + " set to online"));
    }

    @PostMapping("/presence/offline/{userId}")
    public ResponseEntity<ApiResponse<String>> testSetOffline(@PathVariable Long userId) {
        presenceService.setUserOffline(userId);
        return ResponseEntity.ok(ApiResponse.success("User " + userId + " set to offline"));
    }

    @GetMapping("/presence/status/{userId}")
    public ResponseEntity<ApiResponse<Object>> testGetStatus(@PathVariable Long userId) {
        var status = presenceService.getUserStatus(userId);
        var info = presenceService.getPresenceInfo(userId, userId);
        return ResponseEntity.ok(ApiResponse.success(info));
    }
}