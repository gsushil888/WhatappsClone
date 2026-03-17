package com.whatsapp.controller;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.ConversationDto;
import com.whatsapp.service.ConversationService;
import com.whatsapp.util.RequestContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

	private final ConversationService conversationService;

	@GetMapping
	public ResponseEntity<ApiResponse<ConversationDto.ConversationListResponse>> getConversations(
			Authentication authentication,
			@RequestParam(defaultValue = "20") int limit,
			@RequestParam(defaultValue = "0") int offset,
			@RequestParam(defaultValue = "all") String filter) {

		Long userId = (Long) authentication.getPrincipal();
		ConversationDto.ConversationListResponse conversations = conversationService
				.getUserConversations(userId, limit, offset, filter);

		return ResponseEntity.ok(
				ApiResponse.<ConversationDto.ConversationListResponse>builder()
						.success(true).data(conversations)
						.correlationId(RequestContext.getCorrelationId())
						.build());
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ConversationDto.ConversationResponse>> createConversation(
			Authentication authentication,
			@Valid @RequestBody ConversationDto.CreateConversationRequest request) {
		log.info("Create Conv=> "+request.getGroupPictureUrl());
		Long userId = (Long) authentication.getPrincipal();
		ConversationDto.ConversationResponse conversation = conversationService
				.createConversation(userId, request);

		return ResponseEntity
				.ok(ApiResponse.<ConversationDto.ConversationResponse>builder()
						.success(true).data(conversation)
						.correlationId(RequestContext.getCorrelationId())
						.message("Conversation created successfully").build());
	}

	@GetMapping("/{conversationId}")
	public ResponseEntity<ApiResponse<ConversationDto.ConversationDetailsResponse>> getConversationDetails(
			Authentication authentication, @PathVariable Long conversationId) {
		Long userId = (Long) authentication.getPrincipal();
		log.info("Getting conversations list for user " + userId
				+ " with conv id " + conversationId);
		ConversationDto.ConversationDetailsResponse conversation = conversationService
				.getConversationDetails(userId, conversationId);

		return ResponseEntity.ok(
				ApiResponse.<ConversationDto.ConversationDetailsResponse>builder()
						.success(true).data(conversation)
						.correlationId(RequestContext.getCorrelationId())
						.build());
	}

	@PutMapping("/{conversationId}")
	public ResponseEntity<ApiResponse<ConversationDto.ConversationResponse>> updateConversation(
			Authentication authentication, @PathVariable Long conversationId,
			@Valid @RequestBody ConversationDto.UpdateConversationRequest request) {

		Long userId = (Long) authentication.getPrincipal();
		ConversationDto.ConversationResponse response = conversationService
				.updateConversation(userId, conversationId, request);

		return ResponseEntity
				.ok(ApiResponse.<ConversationDto.ConversationResponse>builder()
						.success(true).data(response)
						.correlationId(RequestContext.getCorrelationId())
						.message("Conversation updated successfully").build());
	}

	@PutMapping("/{conversationId}/settings")
	public ResponseEntity<ApiResponse<ConversationDto.ConversationSettingsResponse>> updateConversationSettings(
			Authentication authentication, @PathVariable Long conversationId,
			@Valid @RequestBody ConversationDto.UpdateConversationSettingsRequest request) {

		Long userId = (Long) authentication.getPrincipal();
		ConversationDto.ConversationSettingsResponse response = conversationService
				.updateConversationSettings(userId, conversationId, request);

		return ResponseEntity.ok(
				ApiResponse.<ConversationDto.ConversationSettingsResponse>builder()
						.success(true).data(response)
						.correlationId(RequestContext.getCorrelationId())
						.message("Settings updated successfully").build());
	}

	@PostMapping("/{conversationId}/participants")
	public ResponseEntity<ApiResponse<ConversationDto.AddParticipantsResponse>> addParticipants(
			Authentication authentication, @PathVariable Long conversationId,
			@Valid @RequestBody ConversationDto.AddParticipantsRequest request) {

		Long userId = (Long) authentication.getPrincipal();
		ConversationDto.AddParticipantsResponse response = conversationService
				.addParticipants(userId, conversationId, request);

		return ResponseEntity.ok(
				ApiResponse.<ConversationDto.AddParticipantsResponse>builder()
						.success(true).data(response)
						.correlationId(RequestContext.getCorrelationId())
						.message("Participants added successfully").build());
	}

	@DeleteMapping("/{conversationId}/participants/{participantId}")
	public ResponseEntity<ApiResponse<Void>> removeParticipant(
			Authentication authentication, @PathVariable Long conversationId,
			@PathVariable Long participantId) {

		Long userId = (Long) authentication.getPrincipal();
		conversationService.removeParticipant(userId, conversationId,
				participantId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId())
				.message("Participant removed successfully").build());
	}

	@PostMapping("/{conversationId}/leave")
	public ResponseEntity<ApiResponse<Void>> leaveConversation(
			Authentication authentication, @PathVariable Long conversationId) {

		Long userId = (Long) authentication.getPrincipal();
		conversationService.leaveConversation(userId, conversationId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId())
				.message("Left conversation successfully").build());
	}

	@PostMapping("/{conversationId}/mute")
	public ResponseEntity<ApiResponse<ConversationDto.MuteConversationResponse>> muteConversation(
			Authentication authentication, @PathVariable Long conversationId,
			@Valid @RequestBody ConversationDto.MuteConversationRequest request) {

		Long userId = (Long) authentication.getPrincipal();
		ConversationDto.MuteConversationResponse response = conversationService
				.muteConversation(userId, conversationId, request);

		return ResponseEntity.ok(
				ApiResponse.<ConversationDto.MuteConversationResponse>builder()
						.success(true).data(response)
						.correlationId(RequestContext.getCorrelationId())
						.message("Conversation muted successfully").build());
	}

	@PostMapping("/{conversationId}/unmute")
	public ResponseEntity<ApiResponse<Void>> unmuteConversation(
			Authentication authentication, @PathVariable Long conversationId) {

		Long userId = (Long) authentication.getPrincipal();
		conversationService.unmuteConversation(userId, conversationId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId())
				.message("Conversation unmuted successfully").build());
	}

	@PostMapping("/{conversationId}/archive")
	public ResponseEntity<ApiResponse<Void>> archiveConversation(
			Authentication authentication, @PathVariable Long conversationId) {

		Long userId = (Long) authentication.getPrincipal();
		conversationService.archiveConversation(userId, conversationId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId())
				.message("Conversation archived successfully").build());
	}

	@PostMapping("/{conversationId}/unarchive")
	public ResponseEntity<ApiResponse<Void>> unarchiveConversation(
			Authentication authentication, @PathVariable Long conversationId) {

		Long userId = (Long) authentication.getPrincipal();
		conversationService.unarchiveConversation(userId, conversationId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId())
				.message("Conversation unarchived successfully").build());
	}

	@PostMapping("/{conversationId}/pin")
	public ResponseEntity<ApiResponse<Void>> pinConversation(
			Authentication authentication, @PathVariable Long conversationId) {

		Long userId = (Long) authentication.getPrincipal();
		conversationService.pinConversation(userId, conversationId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId())
				.message("Conversation pinned successfully").build());
	}

	@PostMapping("/{conversationId}/unpin")
	public ResponseEntity<ApiResponse<Void>> unpinConversation(
			Authentication authentication, @PathVariable Long conversationId) {

		Long userId = (Long) authentication.getPrincipal();
		conversationService.unpinConversation(userId, conversationId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId())
				.message("Conversation unpinned successfully").build());
	}

	@PostMapping("/{conversationId}/favorite")
	public ResponseEntity<ApiResponse<Void>> favoriteConversation(
			Authentication authentication, @PathVariable Long conversationId) {

		Long userId = (Long) authentication.getPrincipal();
		conversationService.favoriteConversation(userId, conversationId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId())
				.message("Conversation added to favorites").build());
	}

	@PostMapping("/{conversationId}/unfavorite")
	public ResponseEntity<ApiResponse<Void>> unfavoriteConversation(
			Authentication authentication, @PathVariable Long conversationId) {

		Long userId = (Long) authentication.getPrincipal();
		conversationService.unfavoriteConversation(userId, conversationId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId())
				.message("Conversation removed from favorites").build());
	}

	@DeleteMapping("/{conversationId}")
	public ResponseEntity<ApiResponse<ConversationDto.DeleteConversationResponse>> deleteConversation(
			Authentication authentication, @PathVariable Long conversationId) {

		Long userId = (Long) authentication.getPrincipal();
		ConversationDto.DeleteConversationResponse response = conversationService
				.deleteConversation(userId, conversationId);

		return ResponseEntity.ok(
				ApiResponse.<ConversationDto.DeleteConversationResponse>builder()
						.success(true).data(response)
						.correlationId(RequestContext.getCorrelationId())
						.message("Conversation and all messages deleted successfully")
						.build());
	}
}