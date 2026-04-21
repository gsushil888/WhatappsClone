package com.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CallDto {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CallResponse {
    private Long id;
    private String callType;
    private String callStatus;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationSeconds;
    private String callDirection;
    private String displayName;
    private String profilePictureUrl;
    private Long conversationId;
    private String callToken;
    private List<IceServer> iceServers;
    private List<ParticipantDto> participants;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InitiateCallRequest {
    @NotBlank(message = "Call type is required")
    private String callType; // VOICE, VIDEO

    private Long conversationId;

    @NotEmpty(message = "Participant IDs are required")
    private List<Long> participantIds;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AnswerCallRequest {
    private String webrtcAnswer;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DeclineCallRequest {
    private String reason; // BUSY, DECLINED, etc.
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class JoinCallRequest {
    private String webrtcAnswer;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ToggleMuteRequest {
    private Boolean isMuted;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ToggleVideoRequest {
    private Boolean isVideoEnabled;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WebRTCSignalRequest {
    private String type; // offer, answer, ice-candidate
    private Object data;
    private Long targetParticipantId;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CallParticipantResponse {
    private Long participantId;
    private Long userId;
    private String displayName;
    private String profilePictureUrl;
    private String status;
    private Boolean isMuted;
    private Boolean isVideoEnabled;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class IceServer {
    private List<String> urls;
    private String username;
    private String credential;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ParticipantDto {
    private Long userId;
    private String displayName;
    private String profilePictureUrl;
    private String participantStatus; // CALLING, ANSWERED, DECLINED, BUSY
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CallHistoryResponse {
    private List<CallResponse> calls;
    private ApiResponse.PaginationInfo pagination;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class CallStatisticsResponse {
    private Long totalCalls;
    private Long totalDurationMinutes;
    private Long videoCalls;
    private Long voiceCalls;
    private Long missedCalls;
    private Map<String, Long> callsByDay;
  }
}
