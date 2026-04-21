package com.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class StoryDto {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class StoryResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String content;
    private String mediaUrl;
    private String storyType;
    private LocalDateTime createdAt;
    private Integer viewCount;
    private Boolean isViewed;
    private Boolean hasUnviewedStories;
    private String privacySetting;
    private LocalDateTime expiresAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PostStoryRequest {
    @NotBlank(message = "Story type is required")
    private String storyType; // TEXT, IMAGE, VIDEO

    @Size(max = 1000, message = "Content cannot exceed 1000 characters")
    private String content;

    private String mediaUrl;
    private MessageDto.MediaMetadata mediaMetadata;

    @NotBlank(message = "Privacy setting is required")
    private String privacySetting; // ALL_CONTACTS, CLOSE_FRIENDS, CUSTOM

    private List<Long> allowedViewerIds; // For CUSTOM privacy
    private List<Long> blockedViewerIds; // Users who cannot see this story
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class StoryFeedResponse {
    private List<StoryResponse> stories;
    private ApiResponse.PaginationInfo pagination;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class StoryViewerResponse {
    private Long viewerId;
    private String viewerName;
    private String viewerAvatar;
    private LocalDateTime viewedAt;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class StoryViewersResponse {
    private List<StoryViewerResponse> viewers;
    private Integer totalViewers;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class UserStoriesResponse {
    private Long userId;
    private String userName;
    private String userAvatar;
    private List<StoryResponse> stories;
    private Integer unviewedCount;
    private LocalDateTime lastStoryAt;
  }
}
