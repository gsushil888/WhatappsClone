package com.whatsapp.service;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.MessageDto;
import com.whatsapp.entity.*;
import com.whatsapp.exception.MessageException;
import com.whatsapp.exception.UserException;
import com.whatsapp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

	private final MessageRepository messageRepository;
	private final UserRepository userRepository;
	private final ConversationRepository conversationRepository;
	private final MessageReactionRepository reactionRepository;
	private final MessageStatusRepository messageStatusRepository;
	private final ConversationParticipantRepository conversationParticipantRepository;
	private final ContactRepository contactRepository;
	private final MessageAttachmentRepository messageAttachmentRepository;

	@Transactional(readOnly = true)
	public MessageDto.MessageListResponse getMessages(Long userId,
			Long conversationId, int limit, Long beforeMessageId, Long afterMessageId,
			Map<String, String> deviceInfo, Map<String, String> headers) {

		conversationParticipantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(
						() -> new MessageException(ErrorCode.CONV_NOT_FOUND));

		Pageable pageable = PageRequest.of(0, limit);
		List<Message> messages;
		
		if (beforeMessageId != null) {
			// Scroll up - get older messages
			messages = messageRepository.findMessagesBeforeId(conversationId, beforeMessageId, pageable);
		} else if (afterMessageId != null) {
			// Scroll down - get newer messages
			messages = messageRepository.findMessagesAfterId(conversationId, afterMessageId, pageable);
		} else {
			// Initial load - get latest messages
			messages = messageRepository.findByConversationIdOrderByTimestampDesc(conversationId, pageable);
		}

		List<MessageDto.MessageResponse> messageResponses = messages.stream()
				.map(msg -> mapToMessageResponse(msg, userId)).collect(Collectors.toList());

		long totalCount = messageRepository.countByConversationId(conversationId);
		
		boolean hasMore = messageResponses.size() == limit;
		Long nextCursor = hasMore && !messageResponses.isEmpty() 
				? messageResponses.get(messageResponses.size() - 1).getId() 
				: null;

		return MessageDto.MessageListResponse.builder()
				.messages(messageResponses)
				.pagination(ApiResponse.PaginationInfo.builder()
						.limit(limit)
						.total((int) totalCount)
						.hasNext(hasMore)
						.build())
				.nextCursor(nextCursor)
				.build();
	}

	@Transactional
	public MessageDto.MessageResponse sendMessage(Long userId,
			Long conversationId, MessageDto.SendMessageRequest request,
			Map<String, String> deviceInfo, Map<String, String> headers) {

		User sender = userRepository.findById(userId)
				.orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		Conversation conversation = conversationRepository
				.findById(conversationId).orElseThrow(
						() -> new MessageException(ErrorCode.CONV_NOT_FOUND));

		Message replyToMessage = null;
		if (request.getReplyToMessageId() != null) {
			replyToMessage = messageRepository
					.findById(request.getReplyToMessageId())
					.orElseThrow(() -> new MessageException(
							ErrorCode.MSG_NOT_FOUND));
		}

		Message message = Message.builder().conversation(conversation)
				.sender(sender)
				.type(Message.MessageType
						.valueOf(request.getMessageType().toUpperCase()))
				.content(request.getContent())
				.replyToMessage(replyToMessage)
				.build();

		message = messageRepository.save(message);

		// Handle media attachment if present
		if (request.getMediaUrl() != null && !request.getMediaUrl().isEmpty()) {
			MessageAttachment attachment = MessageAttachment.builder()
					.message(message)
					.type(mapMessageTypeToAttachmentType(message.getType()))
					.fileUrl(request.getMediaUrl())
					.fileName(request.getMediaMetadata() != null ? request.getMediaMetadata().getFileName() : null)
					.fileSize(request.getMediaMetadata() != null ? request.getMediaMetadata().getSize() : null)
					.mimeType(request.getMediaMetadata() != null ? request.getMediaMetadata().getMimeType() : null)
					.width(request.getMediaMetadata() != null ? request.getMediaMetadata().getWidth() : null)
					.height(request.getMediaMetadata() != null ? request.getMediaMetadata().getHeight() : null)
					.duration(request.getMediaMetadata() != null ? request.getMediaMetadata().getDuration() : null)
					.thumbnailUrl(request.getMediaMetadata() != null ? request.getMediaMetadata().getThumbnail() : null)
					.build();
			messageAttachmentRepository.save(attachment);
		}

		return mapToMessageResponse(message, userId);
	}

	@Transactional
	public MessageDto.MessageResponse editMessage(Long userId, Long messageId,
			MessageDto.EditMessageRequest request) {
		Message message = messageRepository.findById(messageId).orElseThrow(
				() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

		if (!message.getSender().getId().equals(userId)) {
			throw new MessageException(ErrorCode.MSG_EDIT_FAILED);
		}

		message.setContent(request.getContent());
		message.setEdited(true);
		message.setEditedAt(LocalDateTime.now());
		message = messageRepository.save(message);

		return mapToMessageResponse(message, userId);
	}

	@Transactional
	public void deleteMessage(Long userId, Long messageId,
			boolean deleteForEveryone) {
		Message message = messageRepository.findById(messageId).orElseThrow(
				() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

		if (!message.getSender().getId().equals(userId)) {
			throw new MessageException(ErrorCode.MSG_DELETE_FAILED);
		}

		if (deleteForEveryone) {
			message.setDeleted(true);
			message.setContent("This message was deleted");
		} else {
			message.setDeletedForSender(true);
		}

		messageRepository.save(message);
	}

	@Transactional
	public void starMessage(Long userId, Long messageId) {
		Message message = messageRepository.findById(messageId).orElseThrow(
				() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

		// Implementation for starring message
		log.info("Message {} starred by user {}", messageId, userId);
	}

	@Transactional
	public void unstarMessage(Long userId, Long messageId) {
		Message message = messageRepository.findById(messageId).orElseThrow(
				() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

		// Implementation for unstarring message
		log.info("Message {} unstarred by user {}", messageId, userId);
	}

	@Transactional
	public void addReaction(Long userId, Long messageId,
			MessageDto.AddReactionRequest request) {
		Message message = messageRepository.findById(messageId).orElseThrow(
				() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		MessageReaction reaction = MessageReaction.builder().message(message)
				.user(user).emoji(request.getEmoji()).build();

		reactionRepository.save(reaction);
	}

	@Transactional
	public void removeReaction(Long userId, Long messageId, String emoji) {
		MessageReaction reaction = reactionRepository
				.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)
				.orElseThrow(
						() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

		reactionRepository.delete(reaction);
	}

	@Transactional(readOnly = true)
	public List<MessageDto.MessageResponse> searchMessages(Long userId,
			Long conversationId, String query, int limit) {
		Pageable pageable = PageRequest.of(0, limit);
		List<Message> messages = messageRepository
				.searchInConversation(conversationId, query, pageable);

		return messages.stream().map(msg -> mapToMessageResponse(msg, userId))
				.collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public MessageDto.MessageListResponse getStarredMessages(Long userId,
			int page, int limit) {
		Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit);
		List<Message> messages = messageRepository.findStarredMessages(userId,
				pageable);

		List<MessageDto.MessageResponse> messageResponses = messages.stream()
				.map(msg -> mapToMessageResponse(msg, userId)).collect(Collectors.toList());

		return MessageDto.MessageListResponse.builder()
				.messages(messageResponses)
				.pagination(ApiResponse.PaginationInfo.builder().page(page)
						.limit(limit).hasNext(messageResponses.size() == limit)
						.build())
				.build();
	}

	private MessageDto.MessageResponse mapToMessageResponse(Message message, Long currentUserId) {
		List<MessageReaction> reactions = reactionRepository
				.findByMessageId(message.getId());
		List<MessageDto.ReactionInfo> reactionInfos = reactions.stream()
				.map(r -> MessageDto.ReactionInfo.builder().emoji(r.getEmoji())
						.userId(r.getUser().getId())
						.displayName(getDisplayName(currentUserId, r.getUser()))
						.createdAt(r.getCreatedAt()).build())
				.collect(Collectors.toList());

		List<MessageStatus> statuses = messageStatusRepository
				.findByMessageId(message.getId());
		Map<String, Object> deliveryStatus = Map.of("sent",
				statuses.stream().filter(
						s -> s.getStatus() == MessageStatus.DeliveryStatus.SENT)
						.count(),
				"delivered",
				statuses.stream().filter(s -> s
						.getStatus() == MessageStatus.DeliveryStatus.DELIVERED)
						.count(),
				"read",
				statuses.stream().filter(
						s -> s.getStatus() == MessageStatus.DeliveryStatus.READ)
						.count());

		// Load attachments from repository to ensure they're fetched
		List<MessageAttachment> attachments = messageAttachmentRepository.findByMessageId(message.getId());
		
		MessageDto.MediaMetadata mediaMetadata = null;
		String mediaUrl = null;
		if (attachments != null && !attachments.isEmpty()) {
			MessageAttachment attachment = attachments.get(0);
			mediaUrl = attachment.getFileUrl();
			mediaMetadata = MessageDto.MediaMetadata.builder()
					.width(attachment.getWidth()).height(attachment.getHeight())
					.size(attachment.getFileSize())
					.mimeType(attachment.getMimeType())
					.duration(attachment.getDuration())
					.thumbnail(attachment.getThumbnailUrl())
					.fileName(attachment.getFileName()).build();
		}

		User sender = message.getSender();
		Optional<Contact> senderContact = contactRepository
				.findByUserIdAndContactUserId(currentUserId, sender.getId());
		String senderName = senderContact.isPresent() 
				? senderContact.get().getDisplayName() 
				: sender.getDisplayName();

		return MessageDto.MessageResponse.builder().id(message.getId())
				.senderId(sender.getId())
				.senderName(senderName)
				.senderMobileNumber(sender.getPhoneNumber())
				.senderAvatar(sender.getProfilePictureUrl())
				.content(message.getContent())
				.messageType(message.getType().name())
				.mediaUrl(mediaUrl)
				.mediaMetadata(mediaMetadata)
				.replyToMessageId(message.getReplyToMessage() != null
						? message.getReplyToMessage().getId()
						: null)
				.isEdited(message.isEdited()).editedAt(message.getEditedAt())
				.reactions(reactionInfos).deliveryStatus(deliveryStatus)
				.createdAt(message.getCreatedAt()).build();
	}

	private String getDisplayName(Long currentUserId, User targetUser) {
		Optional<Contact> contact = contactRepository
				.findByUserIdAndContactUserId(currentUserId, targetUser.getId());
		if (contact.isPresent()) {
			return contact.get().getDisplayName();
		}
		String userDisplayName = targetUser.getDisplayName();
		return (userDisplayName != null && !userDisplayName.trim().isEmpty()) 
				? userDisplayName : targetUser.getPhoneNumber();
	}

	private MessageAttachment.AttachmentType mapMessageTypeToAttachmentType(Message.MessageType messageType) {
		return switch (messageType) {
			case IMAGE -> MessageAttachment.AttachmentType.IMAGE;
			case VIDEO -> MessageAttachment.AttachmentType.VIDEO;
			case AUDIO -> MessageAttachment.AttachmentType.AUDIO;
			case DOCUMENT -> MessageAttachment.AttachmentType.DOCUMENT;
			case STICKER -> MessageAttachment.AttachmentType.STICKER;
			default -> MessageAttachment.AttachmentType.DOCUMENT;
		};
	}
}
