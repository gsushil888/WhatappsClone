package com.whatsapp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.MessageDto;
import com.whatsapp.service.MessageService;
import com.whatsapp.util.RequestContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MessageController {

	private final MessageService messageService;

	@GetMapping("/conversations/{conversationId}/messages")
	public ResponseEntity<ApiResponse<MessageDto.MessageListResponse>> getMessages(Authentication authentication,
			@PathVariable Long conversationId,
			@RequestParam(defaultValue = "50") int limit,
			@RequestParam(required = false) Long beforeMessageId,
			@RequestParam(required = false) Long afterMessageId,
			@RequestHeader Map<String, String> headers) {

		Long userId = (Long) authentication.getPrincipal();
		Map<String, String> deviceInfo = RequestContext.getDeviceInfo();
		if (deviceInfo == null) deviceInfo = new HashMap<>();
		MessageDto.MessageListResponse messages = messageService.getMessages(userId, conversationId, limit,
				beforeMessageId, afterMessageId, deviceInfo, headers);

		return ResponseEntity.ok(ApiResponse.<MessageDto.MessageListResponse>builder().success(true).data(messages)
				.correlationId(RequestContext.getCorrelationId()).build());
	}

	@PostMapping("/conversations/{conversationId}/messages")
	public ResponseEntity<ApiResponse<MessageDto.MessageResponse>> sendMessage(Authentication authentication,
			@PathVariable Long conversationId, @Valid @RequestBody MessageDto.SendMessageRequest request,
			@RequestHeader Map<String, String> headers) {

		Long userId = (Long) authentication.getPrincipal();
		Map<String, String> deviceInfo = RequestContext.getDeviceInfo();
		if (deviceInfo == null) deviceInfo = new HashMap<>();
		MessageDto.MessageResponse message = messageService.sendMessage(userId, conversationId, request, deviceInfo,
				headers);

		return ResponseEntity.ok(ApiResponse.<MessageDto.MessageResponse>builder().success(true).data(message)
				.correlationId(RequestContext.getCorrelationId()).message("Message sent successfully").build());
	}

	@PutMapping("/messages/{messageId}")
	public ResponseEntity<ApiResponse<MessageDto.MessageResponse>> editMessage(Authentication authentication,
			@PathVariable Long messageId, @Valid @RequestBody MessageDto.EditMessageRequest request) {

		Long userId = (Long) authentication.getPrincipal();
		MessageDto.MessageResponse message = messageService.editMessage(userId, messageId, request);

		return ResponseEntity.ok(ApiResponse.<MessageDto.MessageResponse>builder().success(true).data(message)
				.correlationId(RequestContext.getCorrelationId()).message("Message edited successfully").build());
	}

	@DeleteMapping("/messages/{messageId}")
	public ResponseEntity<ApiResponse<Void>> deleteMessage(Authentication authentication, @PathVariable Long messageId,
			@RequestParam(defaultValue = "false") boolean deleteForEveryone) {

		Long userId = (Long) authentication.getPrincipal();
		messageService.deleteMessage(userId, messageId, deleteForEveryone);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId()).message("Message deleted successfully").build());
	}

	@PostMapping("/messages/{messageId}/star")
	public ResponseEntity<ApiResponse<Void>> starMessage(Authentication authentication, @PathVariable Long messageId) {

		Long userId = (Long) authentication.getPrincipal();
		messageService.starMessage(userId, messageId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId()).message("Message starred successfully").build());
	}

	@DeleteMapping("/messages/{messageId}/star")
	public ResponseEntity<ApiResponse<Void>> unstarMessage(Authentication authentication,
			@PathVariable Long messageId) {

		Long userId = (Long) authentication.getPrincipal();
		messageService.unstarMessage(userId, messageId);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId()).message("Message unstarred successfully").build());
	}

	@PostMapping("/messages/{messageId}/reactions")
	public ResponseEntity<ApiResponse<Void>> addReaction(Authentication authentication, @PathVariable Long messageId,
			@Valid @RequestBody MessageDto.AddReactionRequest request) {

		Long userId = (Long) authentication.getPrincipal();
		messageService.addReaction(userId, messageId, request);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId()).message("Reaction added successfully").build());
	}

	@DeleteMapping("/messages/{messageId}/reactions/{emoji}")
	public ResponseEntity<ApiResponse<Void>> removeReaction(Authentication authentication, @PathVariable Long messageId,
			@PathVariable String emoji) {

		Long userId = (Long) authentication.getPrincipal();
		messageService.removeReaction(userId, messageId, emoji);

		return ResponseEntity.ok(ApiResponse.<Void>builder().success(true)
				.correlationId(RequestContext.getCorrelationId()).message("Reaction removed successfully").build());
	}

	@GetMapping("/conversations/{conversationId}/messages/search")
	public ResponseEntity<ApiResponse<List<MessageDto.MessageResponse>>> searchMessages(Authentication authentication,
			@PathVariable Long conversationId, @RequestParam String q, @RequestParam(defaultValue = "20") int limit) {

		Long userId = (Long) authentication.getPrincipal();
		List<MessageDto.MessageResponse> messages = messageService.searchMessages(userId, conversationId, q, limit);

		return ResponseEntity.ok(ApiResponse.<List<MessageDto.MessageResponse>>builder().success(true).data(messages)
				.correlationId(RequestContext.getCorrelationId()).build());
	}

	@GetMapping("/messages/starred")
	public ResponseEntity<ApiResponse<MessageDto.MessageListResponse>> getStarredMessages(Authentication authentication,
			@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int limit) {

		Long userId = (Long) authentication.getPrincipal();
		MessageDto.MessageListResponse messages = messageService.getStarredMessages(userId, page, limit);

		return ResponseEntity.ok(ApiResponse.<MessageDto.MessageListResponse>builder().success(true).data(messages)
				.correlationId(RequestContext.getCorrelationId()).build());
	}
}