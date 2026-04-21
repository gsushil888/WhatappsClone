package com.whatsapp.controller;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.CallDto;
import com.whatsapp.service.CallService;
import com.whatsapp.util.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/calls")
@RequiredArgsConstructor
public class CallController {

  private final CallService callService;

  @PostMapping
  public ResponseEntity<ApiResponse<CallDto.CallResponse>> initiateCall(
      Authentication authentication, @Valid @RequestBody CallDto.InitiateCallRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallResponse call = callService.initiateCall(userId, request);

    return ResponseEntity.ok(ApiResponse.<CallDto.CallResponse>builder().success(true).data(call)
        .correlationId(RequestContext.getCorrelationId()).message("Call initiated successfully")
        .build());
  }

  @PostMapping("/{callId}/answer")
  public ResponseEntity<ApiResponse<CallDto.CallResponse>> answerCall(Authentication authentication,
      @PathVariable Long callId, @Valid @RequestBody CallDto.AnswerCallRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallResponse call = callService.answerCall(userId, callId, request);

    return ResponseEntity.ok(ApiResponse.<CallDto.CallResponse>builder().success(true).data(call)
        .correlationId(RequestContext.getCorrelationId()).message("Call answered successfully")
        .build());
  }

  @PostMapping("/{callId}/decline")
  public ResponseEntity<ApiResponse<CallDto.CallResponse>> declineCall(
      Authentication authentication, @PathVariable Long callId,
      @RequestBody(required = false) CallDto.DeclineCallRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallResponse call = callService.declineCall(userId, callId, request);

    return ResponseEntity.ok(ApiResponse.<CallDto.CallResponse>builder().success(true).data(call)
        .correlationId(RequestContext.getCorrelationId()).message("Call declined successfully")
        .build());
  }

  @PostMapping("/{callId}/end")
  public ResponseEntity<ApiResponse<CallDto.CallResponse>> endCall(Authentication authentication,
      @PathVariable Long callId) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallResponse call = callService.endCall(userId, callId);

    return ResponseEntity.ok(ApiResponse.<CallDto.CallResponse>builder().success(true).data(call)
        .correlationId(RequestContext.getCorrelationId()).message("Call ended successfully")
        .build());
  }

  @PostMapping("/{callId}/join")
  public ResponseEntity<ApiResponse<CallDto.CallResponse>> joinCall(Authentication authentication,
      @PathVariable Long callId, @Valid @RequestBody CallDto.JoinCallRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallResponse call = callService.joinCall(userId, callId, request);

    return ResponseEntity.ok(ApiResponse.<CallDto.CallResponse>builder().success(true).data(call)
        .correlationId(RequestContext.getCorrelationId()).message("Joined call successfully")
        .build());
  }

  @PostMapping("/{callId}/leave")
  public ResponseEntity<ApiResponse<CallDto.CallResponse>> leaveCall(Authentication authentication,
      @PathVariable Long callId) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallResponse call = callService.leaveCall(userId, callId);

    return ResponseEntity.ok(ApiResponse.<CallDto.CallResponse>builder().success(true).data(call)
        .correlationId(RequestContext.getCorrelationId()).message("Left call successfully")
        .build());
  }

  @GetMapping("/{callId}")
  public ResponseEntity<ApiResponse<CallDto.CallResponse>> getCallDetails(
      Authentication authentication, @PathVariable Long callId) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallResponse call = callService.getCallDetails(userId, callId);

    return ResponseEntity.ok(ApiResponse.<CallDto.CallResponse>builder().success(true).data(call)
        .correlationId(RequestContext.getCorrelationId()).build());
  }

  @PostMapping("/{callId}/mute")
  public ResponseEntity<ApiResponse<CallDto.CallParticipantResponse>> toggleMute(
      Authentication authentication, @PathVariable Long callId,
      @RequestBody CallDto.ToggleMuteRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallParticipantResponse participant =
        callService.toggleMute(userId, callId, request.getIsMuted());

    return ResponseEntity.ok(ApiResponse.<CallDto.CallParticipantResponse>builder().success(true)
        .data(participant).correlationId(RequestContext.getCorrelationId())
        .message("Mute status updated").build());
  }

  @PostMapping("/{callId}/video")
  public ResponseEntity<ApiResponse<CallDto.CallParticipantResponse>> toggleVideo(
      Authentication authentication, @PathVariable Long callId,
      @RequestBody CallDto.ToggleVideoRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallParticipantResponse participant =
        callService.toggleVideo(userId, callId, request.getIsVideoEnabled());

    return ResponseEntity.ok(ApiResponse.<CallDto.CallParticipantResponse>builder().success(true)
        .data(participant).correlationId(RequestContext.getCorrelationId())
        .message("Video status updated").build());
  }

  @GetMapping("/history")
  public ResponseEntity<ApiResponse<CallDto.CallHistoryResponse>> getCallHistory(
      Authentication authentication, @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset, @RequestParam(required = false) String type,
      @RequestParam(required = false) String status) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallHistoryResponse history =
        callService.getCallHistory(userId, limit, offset, type, status);

    return ResponseEntity.ok(ApiResponse.<CallDto.CallHistoryResponse>builder().success(true)
        .data(history).correlationId(RequestContext.getCorrelationId()).build());
  }

  @GetMapping("/statistics")
  public ResponseEntity<ApiResponse<CallDto.CallStatisticsResponse>> getCallStatistics(
      Authentication authentication, @RequestParam(defaultValue = "week") String period) {

    Long userId = (Long) authentication.getPrincipal();
    CallDto.CallStatisticsResponse statistics = callService.getCallStatistics(userId, period);

    return ResponseEntity.ok(ApiResponse.<CallDto.CallStatisticsResponse>builder().success(true)
        .data(statistics).correlationId(RequestContext.getCorrelationId()).build());
  }

  @PostMapping("/{callId}/signal")
  public ResponseEntity<ApiResponse<Void>> sendSignal(Authentication authentication,
      @PathVariable Long callId, @Valid @RequestBody CallDto.WebRTCSignalRequest request) {

    Long userId = (Long) authentication.getPrincipal();
    callService.sendWebRTCSignal(userId, callId, request);

    return ResponseEntity.ok(
        ApiResponse.<Void>builder().success(true).correlationId(RequestContext.getCorrelationId())
            .message("Signal sent successfully").build());
  }
}
