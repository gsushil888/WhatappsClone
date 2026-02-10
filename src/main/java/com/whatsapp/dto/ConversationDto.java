package com.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class ConversationDto {

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ConversationResponse {

		private Long id;
		private String title;
		private String type;
		private LocalDateTime lastMessageAt;
		private LastMessageDto lastMessage;
		private Integer unreadCount;
		private Boolean isPinned;
		private Boolean isMuted;
		private Boolean isArchived;
		private String groupPictureUrl;
		private List<ParticipantDto> participants;
		private LocalDateTime createdAt;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class LastMessageDto {
		private Long id;
		private String content;
		private String type;
		private LocalDateTime timestamp;
		private MessageSenderDto sender;
		private String status;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class MessageSenderDto {

		private Long id;
		private String displayName;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class CreateConversationRequest {
		@NotBlank(message = "Conversation type is required")
		private String type; // INDIVIDUAL, GROUP

		private Long participantId; // For individual chats

		@NotEmpty(message = "Participant IDs are required for group chats")
		private List<Long> participantIds; // For group chats

		@Size(max = 255, message = "Title cannot exceed 255 characters")
		private String title;

		@Size(max = 1000, message = "Description cannot exceed 1000 characters")
		private String description;

		private String groupPictureUrl;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UpdateConversationRequest {
		private String title;
		private String description;
		private String groupPictureUrl;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ConversationSettingsRequest {
		private Boolean isMuted;
		private Boolean isPinned;
		private Boolean isArchived;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AddParticipantsRequest {
		@NotEmpty(message = "User IDs are required")
		private List<Long> userIds;
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
		private String participantRole;
		private String customName;
		private Boolean isOnline;
		private LocalDateTime lastActiveAt;
		private LocalDateTime joinedAt;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ConversationListResponse {
		private List<ConversationResponse> conversations;
		private ApiResponse.PaginationInfo pagination;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ConversationDetailsResponse {
		private Long id;
		private String title;
		private String type;
		private String description;
		private String groupPictureUrl;
		private List<ParticipantDto> participants;
		private ConversationSettingsDto settings;
		private LocalDateTime createdAt;
		private UserDto createdBy;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ConversationSettingsDto {
		private Boolean isMuted;
		private Boolean isPinned;
		private Boolean isArchived;
		private LocalDateTime muteUntil;
		private Boolean onlyAdminsCanSend;
		private Boolean onlyAdminsCanEditInfo;
		private Integer disappearingMessages;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class UserDto {

		private Long id;
		private String displayName;
		private String profilePictureUrl;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UpdateConversationSettingsRequest {
		private String title;
		private String description;
		private String groupPictureUrl;
		private Boolean isMuted;
		private Boolean isPinned;
		private Boolean isArchived;
		private Integer muteUntil;
		private Boolean onlyAdminsCanSend;
		private Boolean onlyAdminsCanEditInfo;
		private Integer disappearingMessages;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ConversationSettingsResponse {

		private Long conversationId;
		private ConversationSettingsDto settings;
		private LocalDateTime updatedAt;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class AddParticipantsResponse {
		private List<ParticipantDto> addedParticipants;
		private List<Long> failedUserIds;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class MuteConversationRequest {
		private Integer duration; // in seconds, null for indefinite
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class MuteConversationResponse {
		private Boolean isMuted;
		private LocalDateTime muteUntil;
	}
}