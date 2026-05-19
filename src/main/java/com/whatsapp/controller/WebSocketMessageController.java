package com.whatsapp.controller;

import com.whatsapp.dto.MessageDto;
import com.whatsapp.service.MessageService;
import com.whatsapp.service.PresenceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageController {

	private final MessageService messageService;
	private final SimpMessagingTemplate messagingTemplate;
	private final PresenceService presenceService;

	@MessageMapping("/chat.message")
	public void sendMessage(@Payload MessageDto.SendMessageRequest request,
			SimpMessageHeaderAccessor headerAccessor, Principal principal) {
		String principalName = principal != null ? principal.getName() : "unknown";
		try {
			Long userId = getUserId(principal, headerAccessor);
			String conversationIdStr = headerAccessor.getFirstNativeHeader("conversationId");
			if (conversationIdStr == null || conversationIdStr.isEmpty()) {
				throw new IllegalArgumentException("conversationId header is required");
			}
			Long conversationId = Long.parseLong(conversationIdStr);
			log.info("User {} sending message to conversation {}", userId, conversationId);

			MessageDto.MessageResponse response = messageService
					.sendMessage(userId, conversationId, request, null, null);

			boolean firstMessage = messageService
					.isFirstMessage(conversationId);
			String convType = messageService
					.getConversationType(conversationId);

			// Send personalized message to each participant
			List<Long> allParticipantIds = messageService
					.getAllParticipantIds(conversationId);
			for (Long participantId : allParticipantIds) {
				MessageDto.MessageResponse personalizedResponse = participantId
						.equals(userId)
								? response
								: messageService
										.buildMessageResponseForRecipient(
												response.getId(),
												participantId);
				messagingTemplate.convertAndSendToUser(participantId.toString(),
						"/queue/conversation/" + conversationId,
						personalizedResponse);
			}

			// Push to other participants: new-conversation on first message
			// (INDIVIDUAL), unread-update otherwise
			List<Long> otherParticipantIds = messageService
					.getOtherParticipantIds(conversationId, userId);
			for (Long participantId : otherParticipantIds) {
				String senderName = messageService.resolveSenderName(userId,
						participantId);
				if (firstMessage && "INDIVIDUAL".equals(convType)) {
					Map<String, Object> newConvPayload = new java.util.HashMap<>();
					newConvPayload.put("conversationId", conversationId);
					newConvPayload.put("type", convType);
					newConvPayload.put("unreadCount", 1);
					newConvPayload.put("lastMessage",
							Map.of("id", response.getId(), "content",
									response.getContent() != null
											? response.getContent()
											: "",
									"messageType", response.getMessageType(),
									"senderId", userId, "senderName",
									senderName, "timestamp",
									response.getCreatedAt().toString()));
					messagingTemplate.convertAndSendToUser(
							participantId.toString(), "/queue/new-conversation",
							newConvPayload);
				} else {
					Map<String, Object> unreadUpdate = Map.of("conversationId",
							conversationId, "action", "increment",
							"lastMessage",
							Map.of("id", response.getId(), "content",
									response.getContent() != null
											? response.getContent()
											: "",
									"messageType", response.getMessageType(),
									"senderId", response.getSenderId(),
									"senderName", senderName, "timestamp",
									response.getCreatedAt().toString()));
					messagingTemplate.convertAndSendToUser(
							participantId.toString(), "/queue/unread-update",
							unreadUpdate);
				}
			}

		} catch (Exception e) {
			log.error("Error processing message: ", e);
			messagingTemplate.convertAndSendToUser(principalName, "/queue/errors", Map.of("error", e.getMessage()));
		}
	}

	private Long getUserId(Principal principal,
			SimpMessageHeaderAccessor headerAccessor) {
		if (principal != null) {
			return Long.parseLong(principal.getName());
		}
		Map<String, Object> sessionAttributes = headerAccessor
				.getSessionAttributes();
		if (sessionAttributes != null) {
			Object userId = sessionAttributes.get("userId");
			if (userId != null) {
				return Long.parseLong(userId.toString());
			}
		}
		throw new IllegalStateException("User not authenticated");
	}

	@MessageMapping("/chat.typing")
	public void handleTyping(@Payload TypingIndicator typingIndicator, Principal principal) {
		try {
			Long userId = Long.parseLong(principal.getName());
			log.info("User {} {} typing in conversation {}", userId,
					typingIndicator.getAction(),
					typingIndicator.getConversationId());

			if ("start".equals(typingIndicator.getAction())) {
				presenceService.setUserTyping(userId,
						typingIndicator.getConversationId());
			} else {
				presenceService.setUserStoppedTyping(userId,
						typingIndicator.getConversationId());
			}

		} catch (Exception e) {
			log.error("Error processing typing indicator: ", e);
		}
	}

	@MessageMapping("/chat.read")
	public void markConversationRead(@Payload MarkReadRequest markReadRequest, Principal principal) {
		try {
			Long userId = Long.parseLong(principal.getName());
			List<Long> senderIds = messageService.markConversationAsRead(userId,
					markReadRequest.getConversationId());
			// Notify each unique sender that their messages were read
			for (Long senderId : senderIds) {
				if (!senderId.equals(userId)) {
					messagingTemplate.convertAndSendToUser(senderId.toString(),
							"/queue/message-status",
							Map.of("conversationId",
									markReadRequest.getConversationId(),
									"status", "READ", "readByUserId", userId));
				}
			}
		} catch (Exception e) {
			log.error("Error marking conversation as read: ", e);
		}
	}

	@MessageMapping("/chat.status")
	public void updateMessageStatus(@Payload MessageStatusUpdate statusUpdate, Principal principal) {
		try {
			Long userId = Long.parseLong(principal.getName());
			log.info("User {} updating message {} status to {}", userId,
					statusUpdate.getMessageId(), statusUpdate.getStatus());

			// Save status to DB
			messageService.updateMessageStatus(userId,
					statusUpdate.getMessageId(), statusUpdate.getStatus());

			// Notify the original sender so their tick updates
			Long senderId = messageService
					.getMessageSenderId(statusUpdate.getMessageId());
			if (senderId != null && !senderId.equals(userId)) {
				messagingTemplate.convertAndSendToUser(senderId.toString(),
						"/queue/message-status", statusUpdate);
			}

		} catch (Exception e) {
			log.error("Error updating message status: ", e);
		}
	}

	@MessageMapping("/chat.reaction")
	public void handleReaction(@Payload ReactionUpdate reactionUpdate, Principal principal) {
		try {
			Long userId = Long.parseLong(principal.getName());
			log.info("User {} {} reaction {} on message {}", userId,
					reactionUpdate.getAction(), reactionUpdate.getEmoji(),
					reactionUpdate.getMessageId());

			if ("add".equals(reactionUpdate.getAction())) {
				MessageDto.AddReactionRequest request = new MessageDto.AddReactionRequest();
				request.setEmoji(reactionUpdate.getEmoji());
				messageService.addReaction(userId,
						reactionUpdate.getMessageId(), request);
			} else if ("remove".equals(reactionUpdate.getAction())) {
				messageService.removeReaction(userId,
						reactionUpdate.getMessageId(),
						reactionUpdate.getEmoji());
			}

			// Send personalized reaction update to each participant (correct
			// reactor displayName)
			List<Long> allParticipantIds = messageService
					.getAllParticipantIds(reactionUpdate.getConversationId());
			for (Long participantId : allParticipantIds) {
				String reactorName = messageService.resolveSenderName(userId,
						participantId);
				java.util.Map<String, Object> personalizedReaction = new java.util.HashMap<>();
				personalizedReaction.put("messageId",
						reactionUpdate.getMessageId());
				personalizedReaction.put("conversationId",
						reactionUpdate.getConversationId());
				personalizedReaction.put("emoji", reactionUpdate.getEmoji());
				personalizedReaction.put("action", reactionUpdate.getAction());
				personalizedReaction.put("reactorId", userId);
				personalizedReaction.put("reactorName", reactorName);
				messagingTemplate.convertAndSendToUser(participantId.toString(),
						"/queue/reaction/" + reactionUpdate.getConversationId(),
						personalizedReaction);
			}

		} catch (Exception e) {
			log.error("Error processing reaction: ", e);
			messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", Map.of("error", e.getMessage()));
		}
	}

	@MessageMapping("/presence.update")
	public void updatePresence(@Payload PresenceUpdate presenceUpdate, Principal principal) {
		try {
			Long userId = Long.parseLong(principal.getName());
			log.info("User {} updating presence to {}", userId,
					presenceUpdate.getStatus());

			if ("ONLINE".equals(presenceUpdate.getStatus())) {
				presenceService.setUserOnline(userId,
						presenceUpdate.getDeviceInfo());
			} else {
				presenceService.setUserOffline(userId);
			}

		} catch (Exception e) {
			log.error("Error updating presence: ", e);
		}
	}

	@MessageMapping("/call.initiate")
	public void initiateCall(@Payload CallInitiation callInitiation, Principal principal) {
		try {
			Long userId = Long.parseLong(principal.getName());
			log.info("User {} initiating {} call", userId,
					callInitiation.getCallType());

			messagingTemplate.convertAndSend("/queue/calls/" + userId,
					callInitiation);

		} catch (Exception e) {
			log.error("Error initiating call: ", e);
		}
	}

	@MessageMapping("/call.signal")
	public void handleWebRTCSignal(@Payload WebRTCSignal signal, Principal principal) {
		try {
			Long userId = Long.parseLong(principal.getName());
			log.info("User {} sending WebRTC signal of type {}", userId,
					signal.getSignalType());

			messagingTemplate.convertAndSendToUser(
					signal.getTargetUserId().toString(), "/queue/calls",
					signal);

		} catch (Exception e) {
			log.error("Error handling WebRTC signal: ", e);
		}
	}

	@MessageMapping("/story.post")
	public void postStory(@Payload StoryPost storyPost, Principal principal) {
		try {
			Long userId = Long.parseLong(principal.getName());
			log.info("User {} posting story", userId);

			messagingTemplate.convertAndSend("/queue/stories/" + userId,
					storyPost);

		} catch (Exception e) {
			log.error("Error posting story: ", e);
		}
	}

	@MessageMapping("/story.view")
	public void viewStory(@Payload StoryView storyView, Principal principal) {
		try {
			Long userId = Long.parseLong(principal.getName());
			log.info("User {} viewing story {}", userId,
					storyView.getStoryId());

			messagingTemplate.convertAndSend(
					"/queue/stories/" + storyView.getStoryOwnerId(), storyView);

		} catch (Exception e) {
			log.error("Error viewing story: ", e);
		}
	}

	@Data
	public static class MarkReadRequest {
		private Long conversationId;
	}

	@Data
	public static class TypingIndicator {
		private Long conversationId;
		private String action; // start, stop
	}

	@Data
	public static class MessageStatusUpdate {
		private Long messageId;
		private Long conversationId;
		private String status; // read, delivered
	}

	@Data
	public static class ReactionUpdate {
		private Long messageId;
		private Long conversationId;
		private String emoji;
		private String action; // add, remove
	}

	@Data
	public static class PresenceUpdate {
		private String status; // ONLINE, OFFLINE
		private String deviceInfo;
	}

	@Data
	public static class CallInitiation {
		private String callType; // VIDEO, VOICE
		private Long conversationId;
		private Long[] participantIds;
	}

	@Data
	public static class WebRTCSignal {
		private Long callId;
		private String signalType; // offer, answer, ice-candidate
		private Object signalData;
		private Long targetUserId;
	}

	@Data
	public static class StoryPost {
		private String storyType; // TEXT, IMAGE, VIDEO
		private String content;
		private String mediaUrl;
		private String privacySetting;
	}

	@Data
	public static class StoryView {
		private Long storyId;
		private Long storyOwnerId;
	}
}
