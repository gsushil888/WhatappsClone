package com.whatsapp.controller;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.SearchDto;
import com.whatsapp.dto.UserDto;
import com.whatsapp.service.SearchService;
import com.whatsapp.service.UserService;
import com.whatsapp.util.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class UserController {

  private final UserService userService;
  private final SearchService searchService;

  @GetMapping("/users/me")
  public ResponseEntity<ApiResponse<UserDto.UserProfileResponse>> getCurrentUser(
      Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    log.info("Getting profile for user: {}", userId);

    UserDto.UserProfileResponse user = userService.getUserProfile(userId);

    return ResponseEntity.ok(ApiResponse.<UserDto.UserProfileResponse>builder().success(true)
        .data(user).correlationId(RequestContext.getCorrelationId()).build());
  }

  @PutMapping("/users/me")
  public ResponseEntity<ApiResponse<UserDto.UserProfileResponse>> updateProfile(
      Authentication authentication, @Valid @RequestBody UserDto.UpdateProfileRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    log.info("Updating profile for user: {}", userId);

    UserDto.UserProfileResponse response = userService.updateProfile(userId, request);

    return ResponseEntity.ok(ApiResponse.<UserDto.UserProfileResponse>builder().success(true)
        .data(response).correlationId(RequestContext.getCorrelationId()).build());
  }

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<SearchDto.UniversalSearchResponse>> universalSearch(
      Authentication authentication, @RequestParam String q,
      @RequestParam(defaultValue = "all") String type, @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset) {

    Long userId = (Long) authentication.getPrincipal();
    log.info("Universal search by user: {} with query: {} type: {}", userId, q, type);

    SearchDto.UniversalSearchResponse response = searchService.search(userId, q, type, limit);

    return ResponseEntity.ok(ApiResponse.<SearchDto.UniversalSearchResponse>builder().success(true)
        .data(response).correlationId(RequestContext.getCorrelationId()).build());
  }

  @GetMapping("/users/me/sessions")
  public ResponseEntity<ApiResponse<List<UserDto.SessionResponse>>> getUserSessions(
      Authentication authentication) {

    Long userId = (Long) authentication.getPrincipal();
    log.info("Getting sessions for user: {}", userId);

    List<UserDto.SessionResponse> sessions = userService.getUserSessions(userId);

    return ResponseEntity.ok(ApiResponse.<List<UserDto.SessionResponse>>builder().success(true)
        .data(sessions).correlationId(RequestContext.getCorrelationId()).build());
  }

  @DeleteMapping("/users/me/sessions/{sessionId}")
  public ResponseEntity<ApiResponse<Void>> terminateSession(Authentication authentication,
      @PathVariable String sessionId) {

    Long userId = (Long) authentication.getPrincipal();
    log.info("Terminating session: {} for user: {}", sessionId, userId);

    userService.revokeSession(userId, sessionId);

    return ResponseEntity
        .ok(ApiResponse.<Void>builder().success(true).message("Session terminated successfully")
            .correlationId(RequestContext.getCorrelationId()).build());
  }
}
