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
    private String thumbnailUrl;
    private String storyType;
    // TEXT story
    private String backgroundColor;
    private String textStyle;
    // LINK story
    private String linkUrl;
    private String linkTitle;
    private String linkDescription;
    private String linkPreviewImage;
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
    private String storyType; // TEXT, IMAGE, VIDEO, LINK

    @Size(max = 1000, message = "Content cannot exceed 1000 characters")
    private String content;

    // IMAGE / VIDEO
    private String mediaUrl;
    private String thumbnailUrl;
    private MessageDto.MediaMetadata mediaMetadata;

    // TEXT story styling
    private String backgroundColor; // hex e.g. "#075E54"
    private String textStyle;       // NORMAL, BOLD, ITALIC, HANDWRITING

    // LINK story
    private String linkUrl;
    private String linkTitle;
    private String linkDescription;
    private String linkPreviewImage;

    @NotBlank(message = "Privacy setting is required")
    private String privacySetting; // PUBLIC, CONTACTS, CLOSE_FRIENDS, CUSTOM

    private List<Long> allowedViewerIds;
    private List<Long> blockedViewerIds;
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
