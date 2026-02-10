package com.whatsapp.controller;

import com.whatsapp.dto.MessageDto;
import com.whatsapp.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.message")
    public void sendMessage(@Payload MessageDto.SendMessageRequest request, 
                           SimpMessageHeaderAccessor headerAccessor,
                           Principal principal) {
        try {
            Long userId = Long.parseLong(principal.getName());
            Long conversationId = (Long) headerAccessor.getSessionAttributes().get("conversationId");
            
            MessageDto.MessageResponse response = messageService.sendMessage(userId, conversationId, request, null, null);
            
            messagingTemplate.convertAndSend("/topic/conversation/" + conversationId, response);
            
        } catch (Exception e) {
            log.error("Error processing message: ", e);
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", 
                Map.of("error", e.getMessage()));
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicator typingIndicator,
                            Principal principal) {
        try {
            Long userId = Long.parseLong(principal.getName());
            log.info("User {} typing in conversation {}", userId, typingIndicator.getConversationId());
            
            // Broadcast typing indicator to conversation participants
            messagingTemplate.convertAndSend("/queue/typing/" + userId, typingIndicator);
            
        } catch (Exception e) {
            log.error("Error processing typing indicator: ", e);
        }
    }

    @MessageMapping("/chat.status")
    public void updateMessageStatus(@Payload MessageStatusUpdate statusUpdate,
                                   Principal principal) {
        try {
            Long userId = Long.parseLong(principal.getName());
            log.info("User {} updating message {} status to {}", 
                    userId, statusUpdate.getMessageId(), statusUpdate.getStatus());
            
            // Update message status and notify sender
            messagingTemplate.convertAndSend("/queue/messages/" + userId, statusUpdate);
            
        } catch (Exception e) {
            log.error("Error updating message status: ", e);
        }
    }

    @MessageMapping("/chat.reaction")
    public void handleReaction(@Payload ReactionUpdate reactionUpdate,
                              Principal principal) {
        try {
            Long userId = Long.parseLong(principal.getName());
            log.info("User {} {} reaction {} on message {}", 
                    userId, reactionUpdate.getAction(), reactionUpdate.getEmoji(), reactionUpdate.getMessageId());
            
            // Process reaction and notify conversation participants
            messagingTemplate.convertAndSend("/queue/messages/" + userId, reactionUpdate);
            
        } catch (Exception e) {
            log.error("Error processing reaction: ", e);
        }
    }

    @MessageMapping("/presence.update")
    public void updatePresence(@Payload PresenceUpdate presenceUpdate,
                              Principal principal) {
        try {
            Long userId = Long.parseLong(principal.getName());
            log.info("User {} updating presence to {}", userId, presenceUpdate.getStatus());
            
            // Update user presence and notify contacts
            messagingTemplate.convertAndSend("/queue/presence/" + userId, presenceUpdate);
            
        } catch (Exception e) {
            log.error("Error updating presence: ", e);
        }
    }

    @MessageMapping("/call.initiate")
    public void initiateCall(@Payload CallInitiation callInitiation,
                            Principal principal) {
        try {
            Long userId = Long.parseLong(principal.getName());
            log.info("User {} initiating {} call", userId, callInitiation.getCallType());
            
            // Process call initiation and notify participants
            messagingTemplate.convertAndSend("/queue/calls/" + userId, callInitiation);
            
        } catch (Exception e) {
            log.error("Error initiating call: ", e);
        }
    }

    @MessageMapping("/call.signal")
    public void handleWebRTCSignal(@Payload WebRTCSignal signal,
                                  Principal principal) {
        try {
            Long userId = Long.parseLong(principal.getName());
            log.info("User {} sending WebRTC signal of type {}", userId, signal.getSignalType());
            
            // Forward WebRTC signal to target user
            messagingTemplate.convertAndSendToUser(
                signal.getTargetUserId().toString(), 
                "/queue/calls", 
                signal
            );
            
        } catch (Exception e) {
            log.error("Error handling WebRTC signal: ", e);
        }
    }

    @MessageMapping("/story.post")
    public void postStory(@Payload StoryPost storyPost,
                         Principal principal) {
        try {
            Long userId = Long.parseLong(principal.getName());
            log.info("User {} posting story", userId);
            
            // Process story post and notify contacts
            messagingTemplate.convertAndSend("/queue/stories/" + userId, storyPost);
            
        } catch (Exception e) {
            log.error("Error posting story: ", e);
        }
    }

    @MessageMapping("/story.view")
    public void viewStory(@Payload StoryView storyView,
                         Principal principal) {
        try {
            Long userId = Long.parseLong(principal.getName());
            log.info("User {} viewing story {}", userId, storyView.getStoryId());
            
            // Process story view and notify story owner
            messagingTemplate.convertAndSend("/queue/stories/" + storyView.getStoryOwnerId(), storyView);
            
        } catch (Exception e) {
            log.error("Error viewing story: ", e);
        }
    }

    // Inner classes for message payloads
    public static class TypingIndicator {
        private Long conversationId;
        private String action; // start, stop
        
        // Getters and setters
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }

    public static class MessageStatusUpdate {
        private Long messageId;
        private Long conversationId;
        private String status; // read, delivered
        
        // Getters and setters
        public Long getMessageId() { return messageId; }
        public void setMessageId(Long messageId) { this.messageId = messageId; }
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ReactionUpdate {
        private Long messageId;
        private Long conversationId;
        private String emoji;
        private String action; // add, remove
        
        // Getters and setters
        public Long getMessageId() { return messageId; }
        public void setMessageId(Long messageId) { this.messageId = messageId; }
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public String getEmoji() { return emoji; }
        public void setEmoji(String emoji) { this.emoji = emoji; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
    }

    public static class PresenceUpdate {
        private String status; // online, offline
        private String lastActiveAt;
        
        // Getters and setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getLastActiveAt() { return lastActiveAt; }
        public void setLastActiveAt(String lastActiveAt) { this.lastActiveAt = lastActiveAt; }
    }

    public static class CallInitiation {
        private String callType; // VIDEO, VOICE
        private Long conversationId;
        private Long[] participantIds;
        
        // Getters and setters
        public String getCallType() { return callType; }
        public void setCallType(String callType) { this.callType = callType; }
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
        public Long[] getParticipantIds() { return participantIds; }
        public void setParticipantIds(Long[] participantIds) { this.participantIds = participantIds; }
    }

    public static class WebRTCSignal {
        private Long callId;
        private String signalType; // offer, answer, ice-candidate
        private Object signalData;
        private Long targetUserId;
        
        // Getters and setters
        public Long getCallId() { return callId; }
        public void setCallId(Long callId) { this.callId = callId; }
        public String getSignalType() { return signalType; }
        public void setSignalType(String signalType) { this.signalType = signalType; }
        public Object getSignalData() { return signalData; }
        public void setSignalData(Object signalData) { this.signalData = signalData; }
        public Long getTargetUserId() { return targetUserId; }
        public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    }

    public static class StoryPost {
        private String storyType; // TEXT, IMAGE, VIDEO
        private String content;
        private String mediaUrl;
        private String privacySetting;
        
        // Getters and setters
        public String getStoryType() { return storyType; }
        public void setStoryType(String storyType) { this.storyType = storyType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getMediaUrl() { return mediaUrl; }
        public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
        public String getPrivacySetting() { return privacySetting; }
        public void setPrivacySetting(String privacySetting) { this.privacySetting = privacySetting; }
    }

    public static class StoryView {
        private Long storyId;
        private Long storyOwnerId;
        
        // Getters and setters
        public Long getStoryId() { return storyId; }
        public void setStoryId(Long storyId) { this.storyId = storyId; }
        public Long getStoryOwnerId() { return storyOwnerId; }
        public void setStoryOwnerId(Long storyOwnerId) { this.storyOwnerId = storyOwnerId; }
    }
}