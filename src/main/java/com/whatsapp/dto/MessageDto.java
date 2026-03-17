package com.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MessageDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageResponse {
        private Long id;
        private Long senderId;
        private String senderName;
        private String senderMobileNumber;
        private String senderAvatar;
        private String content;
        private String messageType;
        private String mediaUrl;
        private MediaMetadata mediaMetadata;
        private LocationData location;
        private Long replyToMessageId;
        private MessageResponse replyToMessage;
        private Boolean isEdited;
        private LocalDateTime editedAt;
        private Boolean isStarred;
        private Map<String, Object> deliveryStatus;
        private List<ReactionInfo> reactions;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        @NotBlank(message = "Message type is required")
        private String messageType; // TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION, CONTACT

        @Size(max = 4000, message = "Content cannot exceed 4000 characters")
        private String content;

        private String mediaUrl;
        private MediaMetadata mediaMetadata;
        private LocationData location;
        private Long replyToMessageId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditMessageRequest {
        @NotBlank(message = "Content is required")
        @Size(max = 4000, message = "Content cannot exceed 4000 characters")
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddReactionRequest {
        @NotBlank(message = "Emoji is required")
        private String emoji;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MediaMetadata {
        private Integer width;
        private Integer height;
        private Long size;
        private String mimeType;
        private Integer duration; // For audio/video
        private String thumbnail;
        private String fileName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LocationData {
        private Double latitude;
        private Double longitude;
        private String address;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageListResponse {
        private List<MessageResponse> messages;
        private ApiResponse.PaginationInfo pagination;
        private Long nextCursor; // ID of last message for cursor-based pagination
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ReactionInfo {
        private String emoji;
        private Long userId;
        private String displayName;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryStatus {
        private String status; // SENT, DELIVERED, READ
        private LocalDateTime timestamp;
    }
}