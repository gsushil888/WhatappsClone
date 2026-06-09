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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	public MessageDto.MessageListResponse getMessages(Long userId, Long conversationId, int limit, Long beforeMessageId,
			Long afterMessageId, Map<String, String> deviceInfo, Map<String, String> headers) {
		log.info("Getting messages of user {} of conversation {}", userId, conversationId);
		ConversationParticipant participant = conversationParticipantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new MessageException(ErrorCode.CONV_NOT_FOUND));

		LocalDateTime clearedAt = participant.getClearedAt();
		LocalDateTime removedAt = participant.getStatus() == ConversationParticipant.ParticipantStatus.REMOVED
				? participant.getRemovedAt()
				: null;
		LocalDateTime gapStart = participant.getGapStart();
		LocalDateTime readdedAt = participant.getReaddedAt();
		Pageable pageable = PageRequest.of(0, limit);
		List<Message> messages;

		if (beforeMessageId != null) {
			messages = messageRepository.findMessagesBeforeId(conversationId, beforeMessageId, clearedAt, removedAt,
					gapStart, readdedAt, pageable);
		} else if (afterMessageId != null) {
			messages = messageRepository.findMessagesAfterId(conversationId, afterMessageId, clearedAt, removedAt,
					gapStart, readdedAt, pageable);
		} else {
			messages = messageRepository.findByConversationIdOrderByTimestampDesc(conversationId, clearedAt, removedAt,
					gapStart, readdedAt, pageable);
		}

		if (messages.isEmpty()) {
			return MessageDto.MessageListResponse.builder().messages(List.of())
					.pagination(ApiResponse.PaginationInfo.builder().limit(limit).hasNext(false).build())
					.nextCursor(null).build();
		}

		// Bulk-fetch reactions, statuses, attachments — 3 queries instead of 3*N
		List<Long> messageIds = messages.stream().map(Message::getId).collect(Collectors.toList());

		Map<Long, List<MessageReaction>> reactionsByMsgId = reactionRepository.findByMessageIdIn(messageIds)
				.stream().collect(Collectors.groupingBy(r -> r.getMessage().getId()));

		Map<Long, List<MessageStatus>> statusesByMsgId = messageStatusRepository.findByMessageIdIn(messageIds)
				.stream().collect(Collectors.groupingBy(s -> s.getMessage().getId()));

		Map<Long, List<MessageAttachment>> attachmentsByMsgId = messageAttachmentRepository.findByMessageIdIn(messageIds)
				.stream().collect(Collectors.groupingBy(a -> a.getMessage().getId()));

		// Bulk-fetch contacts for all unique senders — 1 query instead of N
		Set<Long> senderIds = messages.stream().map(m -> m.getSender().getId()).collect(Collectors.toSet());
		Map<Long, Contact> contactBySenderId = contactRepository
				.findByUserIdAndContactUserIdIn(userId, List.copyOf(senderIds)).stream()
				.collect(Collectors.toMap(c -> c.getContactUser().getId(), c -> c, (a, b) -> a));

		List<MessageDto.MessageResponse> messageResponses = messages.stream().map(msg -> {
			List<MessageReaction> reactions = reactionsByMsgId.getOrDefault(msg.getId(), List.of());
			List<MessageStatus> statuses = statusesByMsgId.getOrDefault(msg.getId(), List.of());
			List<MessageAttachment> attachments = attachmentsByMsgId.getOrDefault(msg.getId(), List.of());

			List<MessageDto.ReactionInfo> reactionInfos = reactions.stream()
					.map(r -> MessageDto.ReactionInfo.builder().emoji(r.getEmoji()).userId(r.getUser().getId())
							.displayName(resolveNameFromContact(r.getUser(), contactBySenderId.get(r.getUser().getId())))
							.attachmentId(r.getAttachment() != null ? r.getAttachment().getId() : null)
							.createdAt(r.getCreatedAt()).build())
					.collect(Collectors.toList());

			Map<String, Object> deliveryStatus = Map.of(
					"sent", statuses.stream().filter(s -> s.getStatus() == MessageStatus.DeliveryStatus.SENT).count(),
					"delivered", statuses.stream().filter(s -> s.getStatus() == MessageStatus.DeliveryStatus.DELIVERED).count(),
					"read", statuses.stream().filter(s -> s.getStatus() == MessageStatus.DeliveryStatus.READ).count());

			MessageDto.MediaMetadata mediaMetadata = null;
			String mediaUrl = null;
			List<MessageDto.AttachmentInfo> attachmentInfos = null;
			if (!attachments.isEmpty()) {
				List<MessageAttachment> sorted = attachments.stream()
						.sorted(java.util.Comparator.comparingInt(a -> (a.getSortOrder() != null ? a.getSortOrder() : 0)))
						.collect(Collectors.toList());
				MessageAttachment att = sorted.get(0);
				mediaUrl = att.getFileUrl();
				mediaMetadata = MessageDto.MediaMetadata.builder().width(att.getWidth()).height(att.getHeight())
						.size(att.getFileSize()).mimeType(att.getMimeType()).duration(att.getDuration())
						.thumbnail(att.getThumbnailUrl()).fileName(att.getFileName()).build();
				attachmentInfos = sorted.stream().map(a -> MessageDto.AttachmentInfo.builder()
						.id(a.getId()).fileUrl(a.getFileUrl()).fileName(a.getFileName()).fileSize(a.getFileSize())
						.mimeType(a.getMimeType()).width(a.getWidth()).height(a.getHeight())
						.duration(a.getDuration()).thumbnailUrl(a.getThumbnailUrl())
						.type(a.getType() != null ? a.getType().name() : null).build())
						.collect(Collectors.toList());
			}

			User sender = msg.getSender();
			String senderName = resolveNameFromContact(sender, contactBySenderId.get(sender.getId()));

			return MessageDto.MessageResponse.builder().id(msg.getId()).senderId(sender.getId())
					.senderName(senderName).senderMobileNumber(sender.getPhoneNumber())
					.senderAvatar(sender.getProfilePictureUrl()).content(msg.getContent())
					.messageType(msg.getType().name()).mediaUrl(mediaUrl).mediaMetadata(mediaMetadata)
					.attachments(attachmentInfos)
					.replyToMessageId(msg.getReplyToMessage() != null ? msg.getReplyToMessage().getId() : null)
					.isEdited(msg.isEdited()).editedAt(msg.getEditedAt()).reactions(reactionInfos)
					.deliveryStatus(deliveryStatus).createdAt(msg.getCreatedAt()).build();
		}).collect(Collectors.toList());

		long totalCount = messageRepository.countByConversationId(conversationId, clearedAt, removedAt, gapStart,
				readdedAt);
		boolean hasMore = messageResponses.size() == limit;
		Long nextCursor = hasMore ? messageResponses.get(messageResponses.size() - 1).getId() : null;
		log.info("Successfully fetched {} messages of user {} of conversation {}", messageResponses.size(), userId,
				conversationId);
		return MessageDto.MessageListResponse.builder().messages(messageResponses).pagination(
				ApiResponse.PaginationInfo.builder().limit(limit).total((int) totalCount).hasNext(hasMore).build())
				.nextCursor(nextCursor).build();
	}

	private String resolveNameFromContact(User targetUser, Contact contact) {
		if (contact != null && contact.getDisplayName() != null && !contact.getDisplayName().trim().isEmpty()) {
			return contact.getDisplayName();
		}
		if (targetUser.getDisplayName() != null && !targetUser.getDisplayName().trim().isEmpty()) {
			return targetUser.getDisplayName();
		}
		String fullName = Stream.of(targetUser.getFirstName(), targetUser.getLastName())
				.filter(s -> s != null && !s.trim().isEmpty()).collect(Collectors.joining(" "));
		return !fullName.isEmpty() ? fullName : targetUser.getPhoneNumber();
	}

	@Transactional
	public MessageDto.MessageResponse sendMessage(Long userId, Long conversationId,
			MessageDto.SendMessageRequest request, Map<String, String> deviceInfo, Map<String, String> headers) {

		User sender = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		Conversation conversation = conversationRepository.findById(conversationId)
				.orElseThrow(() -> new MessageException(ErrorCode.CONV_NOT_FOUND));

		// Block REMOVED or LEFT participants from sending
		ConversationParticipant senderParticipant = conversationParticipantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new MessageException(ErrorCode.CONV_NOT_FOUND));
		if (senderParticipant.getStatus() != ConversationParticipant.ParticipantStatus.ACTIVE) {
			throw new MessageException(ErrorCode.MSG_SEND_FAILED);
		}

		Message replyToMessage = null;
		if (request.getReplyToMessageId() != null) {
			replyToMessage = messageRepository.findById(request.getReplyToMessageId())
					.orElseThrow(() -> new MessageException(ErrorCode.MSG_NOT_FOUND));
		}

		Message message = Message.builder().conversation(conversation).sender(sender)
				.type(Message.MessageType.valueOf(request.getMessageType().toUpperCase())).content(request.getContent())
				.replyToMessage(replyToMessage).build();

		message = messageRepository.save(message);
		messageStatusRepository.createStatusForParticipants(message.getId());

		// Re-surface only for INDIVIDUAL conversations where user deleted (LEFT)
		// For GROUP: LEFT = voluntarily left, REMOVED = kicked — neither should be
		// re-surfaced
		if (conversation.getType() == Conversation.ConversationType.INDIVIDUAL) {
			conversationParticipantRepository.findByConversationId(conversation.getId()).stream()
					.filter(p -> p.getStatus() == ConversationParticipant.ParticipantStatus.LEFT).forEach(p -> {
						p.setStatus(ConversationParticipant.ParticipantStatus.ACTIVE);
						conversationParticipantRepository.save(p);
					});
		}
		// Handle multiple attachments
		if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
			List<MessageDto.AttachmentInfo> infos = request.getAttachments();
			for (int i = 0; i < infos.size(); i++) {
				MessageDto.AttachmentInfo info = infos.get(i);
				MessageAttachment.AttachmentType attType = info.getType() != null
						? MessageAttachment.AttachmentType.valueOf(info.getType().toUpperCase())
						: mapMessageTypeToAttachmentType(message.getType());
				MessageAttachment attachment = MessageAttachment.builder().message(message).type(attType)
						.fileUrl(info.getFileUrl()).fileName(info.getFileName()).fileSize(info.getFileSize())
						.mimeType(info.getMimeType()).width(info.getWidth()).height(info.getHeight())
						.duration(info.getDuration()).thumbnailUrl(info.getThumbnailUrl()).sortOrder(i).build();
				messageAttachmentRepository.save(attachment);
			}
		} else if (request.getMediaUrl() != null && !request.getMediaUrl().isEmpty()) {
			// backward-compat: single mediaUrl
			MessageAttachment attachment = MessageAttachment.builder().message(message)
					.type(mapMessageTypeToAttachmentType(message.getType())).fileUrl(request.getMediaUrl())
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
	public MessageDto.MessageResponse editMessage(Long userId, Long messageId, MessageDto.EditMessageRequest request) {
		Message message = messageRepository.findById(messageId)
				.orElseThrow(() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

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
	public void deleteMessage(Long userId, Long messageId, boolean deleteForEveryone) {
		Message message = messageRepository.findById(messageId)
				.orElseThrow(() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

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
		Message message = messageRepository.findById(messageId)
				.orElseThrow(() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

		// Implementation for starring message
		log.info("Message {} starred by user {}", messageId, userId);
	}

	@Transactional
	public void unstarMessage(Long userId, Long messageId) {
		Message message = messageRepository.findById(messageId)
				.orElseThrow(() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

		// Implementation for unstarring message
		log.info("Message {} unstarred by user {}", messageId, userId);
	}

	@Transactional
	public void addReaction(Long userId, Long messageId, MessageDto.AddReactionRequest request) {
		Message message = messageRepository.findById(messageId)
				.orElseThrow(() -> new MessageException(ErrorCode.MSG_NOT_FOUND));

		User user = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		MessageAttachment attachment = null;
		if (request.getAttachmentId() != null) {
			attachment = messageAttachmentRepository.findById(request.getAttachmentId()).orElse(null);
		}

		MessageReaction reaction = MessageReaction.builder().message(message).user(user).emoji(request.getEmoji())
				.attachment(attachment).build();

		reactionRepository.save(reaction);
	}

	@Transactional
	public void removeReaction(Long userId, Long messageId, String emoji, Long attachmentId) {
		MessageReaction reaction = reactionRepository
				.findByMessageIdAndUserIdAndEmojiAndAttachment(messageId, userId, emoji, attachmentId)
				.orElseThrow(() -> new MessageException(ErrorCode.MSG_NOT_FOUND));
		reactionRepository.delete(reaction);
	}

	@Transactional(readOnly = true)
	public List<MessageDto.MessageResponse> searchMessages(Long userId, Long conversationId, String query, int limit) {
		ConversationParticipant searchParticipant = conversationParticipantRepository
				.findByConversationIdAndUserId(conversationId, userId).orElse(null);
		LocalDateTime clearedAt = searchParticipant != null ? searchParticipant.getClearedAt() : null;
		LocalDateTime removedAt = searchParticipant != null
				&& searchParticipant.getStatus() == ConversationParticipant.ParticipantStatus.REMOVED
						? searchParticipant.getRemovedAt()
						: null;
		LocalDateTime gapStart = searchParticipant != null ? searchParticipant.getGapStart() : null;
		LocalDateTime readdedAt = searchParticipant != null ? searchParticipant.getReaddedAt() : null;
		Pageable pageable = PageRequest.of(0, limit);
		List<Message> messages = messageRepository.searchInConversation(conversationId, query, clearedAt, removedAt,
				gapStart, readdedAt, pageable);
		return messages.stream().map(msg -> mapToMessageResponse(msg, userId)).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public MessageDto.MessageListResponse getStarredMessages(Long userId, int page, int limit) {
		Pageable pageable = PageRequest.of(Math.max(0, page - 1), limit);
		List<Message> messages = messageRepository.findStarredMessages(userId, pageable);

		List<MessageDto.MessageResponse> messageResponses = messages.stream()
				.map(msg -> mapToMessageResponse(msg, userId)).collect(Collectors.toList());

		return MessageDto.MessageListResponse.builder().messages(messageResponses).pagination(ApiResponse.PaginationInfo
				.builder().page(page).limit(limit).hasNext(messageResponses.size() == limit).build()).build();
	}

	private MessageDto.MessageResponse mapToMessageResponse(Message message, Long currentUserId) {
		List<MessageReaction> reactions = reactionRepository.findByMessageId(message.getId());
		List<MessageDto.ReactionInfo> reactionInfos = reactions.stream()
				.map(r -> MessageDto.ReactionInfo.builder().emoji(r.getEmoji()).userId(r.getUser().getId())
						.displayName(getDisplayName(currentUserId, r.getUser()))
						.attachmentId(r.getAttachment() != null ? r.getAttachment().getId() : null)
						.createdAt(r.getCreatedAt()).build())
				.collect(Collectors.toList());

		List<MessageStatus> statuses = messageStatusRepository.findByMessageId(message.getId());
		Map<String, Object> deliveryStatus = Map.of("sent",
				statuses.stream().filter(s -> s.getStatus() == MessageStatus.DeliveryStatus.SENT).count(), "delivered",
				statuses.stream().filter(s -> s.getStatus() == MessageStatus.DeliveryStatus.DELIVERED).count(), "read",
				statuses.stream().filter(s -> s.getStatus() == MessageStatus.DeliveryStatus.READ).count());

		// Load attachments from repository to ensure they're fetched
		List<MessageAttachment> attachments = messageAttachmentRepository.findByMessageId(message.getId());

		List<MessageDto.AttachmentInfo> attachmentInfos = attachments == null ? List.of()
				: attachments.stream()
						.sorted(java.util.Comparator.comparingInt(a -> (a.getSortOrder() != null ? a.getSortOrder() : 0)))
						.map(a -> MessageDto.AttachmentInfo.builder()
						.id(a.getId()).fileUrl(a.getFileUrl()).fileName(a.getFileName()).fileSize(a.getFileSize())
						.mimeType(a.getMimeType()).width(a.getWidth()).height(a.getHeight())
						.duration(a.getDuration()).thumbnailUrl(a.getThumbnailUrl())
						.type(a.getType() != null ? a.getType().name() : null).build())
						.collect(Collectors.toList());

		String mediaUrl = attachmentInfos.isEmpty() ? null : attachmentInfos.get(0).getFileUrl();
		MessageDto.MediaMetadata mediaMetadata = attachmentInfos.isEmpty() ? null
				: MessageDto.MediaMetadata.builder().width(attachmentInfos.get(0).getWidth())
						.height(attachmentInfos.get(0).getHeight()).size(attachmentInfos.get(0).getFileSize())
						.mimeType(attachmentInfos.get(0).getMimeType()).duration(attachmentInfos.get(0).getDuration())
						.thumbnail(attachmentInfos.get(0).getThumbnailUrl()).fileName(attachmentInfos.get(0).getFileName()).build();

		User sender = message.getSender();
		String senderName = resolveName(currentUserId, sender);

		return MessageDto.MessageResponse.builder().id(message.getId()).senderId(sender.getId()).senderName(senderName)
				.senderMobileNumber(sender.getPhoneNumber()).senderAvatar(sender.getProfilePictureUrl())
				.content(message.getContent()).messageType(message.getType().name()).mediaUrl(mediaUrl)
				.mediaMetadata(mediaMetadata)
				.attachments(attachmentInfos.isEmpty() ? null : attachmentInfos)
				.replyToMessageId(message.getReplyToMessage() != null ? message.getReplyToMessage().getId() : null)
				.isEdited(message.isEdited()).editedAt(message.getEditedAt()).reactions(reactionInfos)
				.deliveryStatus(deliveryStatus).createdAt(message.getCreatedAt()).build();
	}

	private String getDisplayName(Long currentUserId, User targetUser) {
		return resolveName(currentUserId, targetUser);
	}

	private String resolveName(Long viewerUserId, User targetUser) {
		Optional<Contact> contact = contactRepository.findByUserIdAndContactUserId(viewerUserId, targetUser.getId());
		if (contact.isPresent() && contact.get().getDisplayName() != null
				&& !contact.get().getDisplayName().trim().isEmpty()) {
			return contact.get().getDisplayName();
		}
		if (targetUser.getDisplayName() != null && !targetUser.getDisplayName().trim().isEmpty()) {
			return targetUser.getDisplayName();
		}
		String fullName = Stream.of(targetUser.getFirstName(), targetUser.getLastName())
				.filter(s -> s != null && !s.trim().isEmpty()).collect(Collectors.joining(" "));
		return !fullName.isEmpty() ? fullName : targetUser.getPhoneNumber();
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

	@Transactional
	public List<Long> markConversationAsRead(Long userId, Long conversationId) {
		List<Long> senderIds = messageStatusRepository.findUnreadSenderIds(conversationId, userId);
		messageStatusRepository.markAllAsRead(conversationId, userId, LocalDateTime.now());
		LocalDateTime clearedAt = conversationParticipantRepository
				.findByConversationIdAndUserId(conversationId, userId).map(ConversationParticipant::getClearedAt)
				.orElse(null);
		List<Message> lastMessages = messageRepository.findLastMessageByConversationId(conversationId, clearedAt, null,
				null, null, PageRequest.of(0, 1));
		Long lastMessageId = lastMessages.isEmpty() ? null : lastMessages.get(0).getId();
		conversationParticipantRepository.updateLastRead(conversationId, userId, LocalDateTime.now(), lastMessageId);
		return senderIds;
	}

	// Returns map of senderId -> lastMessageId they sent that was just marked read
	// Used by WebSocket to push exact messageId in the tick update to each sender
	@Transactional
	public Map<Long, Long> markConversationAsReadWithLastMessage(Long userId, Long conversationId) {
		Map<Long, Long> senderLastMessageMap = messageStatusRepository.findUnreadSenderLastMessageIds(conversationId,
				userId);
		messageStatusRepository.markAllAsRead(conversationId, userId, LocalDateTime.now());
		LocalDateTime clearedAt = conversationParticipantRepository
				.findByConversationIdAndUserId(conversationId, userId).map(ConversationParticipant::getClearedAt)
				.orElse(null);
		List<Message> lastMessages = messageRepository.findLastMessageByConversationId(conversationId, clearedAt, null,
				null, null, PageRequest.of(0, 1));
		Long lastMessageId = lastMessages.isEmpty() ? null : lastMessages.get(0).getId();
		conversationParticipantRepository.updateLastRead(conversationId, userId, LocalDateTime.now(), lastMessageId);
		return senderLastMessageMap;
	}

	@Transactional(readOnly = true)
	public Long getConversationIdByMessageId(Long messageId) {
		return messageRepository.findById(messageId).map(m -> m.getConversation().getId()).orElse(null);
	}

	@Transactional
	public void updateMessageStatus(Long userId, Long messageId, String status) {
		messageStatusRepository.updateMessageStatus(messageId, userId,
				MessageStatus.DeliveryStatus.valueOf(status.toUpperCase()), LocalDateTime.now());
	}

	@Transactional(readOnly = true)
	public Long getMessageSenderId(Long messageId) {
		return messageRepository.findById(messageId).map(m -> m.getSender().getId()).orElse(null);
	}

	@Transactional(readOnly = true)
	public boolean isContact(Long viewerUserId, Long targetUserId) {
		return contactRepository.findByUserIdAndContactUserId(viewerUserId, targetUserId).isPresent();
	}

	@Transactional(readOnly = true)
	public String resolveSenderName(Long senderUserId, Long viewerUserId) {
		User sender = userRepository.findById(senderUserId)
				.orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
		return resolveName(viewerUserId, sender);
	}

	@Transactional(readOnly = true)
	public List<Long> getAllParticipantIds(Long conversationId) {
		return conversationParticipantRepository
				.findByConversationIdAndStatus(conversationId, ConversationParticipant.ParticipantStatus.ACTIVE)
				.stream().map(p -> p.getUser().getId()).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public MessageDto.MessageResponse buildMessageResponseForRecipient(Long messageId, Long recipientId) {
		Message message = messageRepository.findById(messageId)
				.orElseThrow(() -> new MessageException(ErrorCode.MSG_NOT_FOUND));
		return mapToMessageResponse(message, recipientId);
	}

	@Transactional(readOnly = true)
	public boolean wasResurfaced(Long conversationId, Long userId) {
		// A participant was re-surfaced if they were LEFT and just became ACTIVE again
		// We detect this by checking: status=ACTIVE AND clearedAt is set AND leftAt was
		// recently cleared
		return conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId)
				.map(p -> p.getStatus() == ConversationParticipant.ParticipantStatus.ACTIVE && p.getClearedAt() != null
						&& p.getLeftAt() == null) // leftAt cleared on re-surface
				.orElse(false);
	}

	@Transactional(readOnly = true)
	public boolean isNewConversationForParticipant(Long conversationId, Long participantId) {
		// new-conversation fires when the conversation should appear fresh on
		// participant's screen:
		// case 1 — no messages exist before this one (truly first message ever)
		// case 2 — participant had deleted/left (clearedAt set, leftAt cleared =
		// re-surfaced)
		return conversationParticipantRepository.findByConversationIdAndUserId(conversationId, participantId).map(p -> {
			boolean resurfaced = p.getStatus() == ConversationParticipant.ParticipantStatus.ACTIVE
					&& p.getClearedAt() != null && p.getLeftAt() == null;
			// count messages visible to this participant (respects clearedAt)
			long visibleCount = messageRepository.countByConversationId(conversationId, p.getClearedAt(), null, null,
					null);
			boolean firstEver = visibleCount == 1; // only the message just sent
			return firstEver || resurfaced;
		}).orElse(false);
	}

	@Transactional(readOnly = true)
	public String getConversationTitle(Long conversationId) {
		return conversationRepository.findById(conversationId).map(c -> c.getName()).orElse(null);
	}

	@Transactional(readOnly = true)
	public String getConversationGroupImage(Long conversationId) {
		return conversationRepository.findById(conversationId).map(c -> c.getGroupImageUrl()).orElse(null);
	}

	@Transactional(readOnly = true)
	public String getSenderProfilePicture(Long userId) {
		return userRepository.findById(userId).map(User::getProfilePictureUrl).orElse(null);
	}

	@Transactional(readOnly = true)
	public String getSenderMobileNumber(Long userId) {
		return userRepository.findById(userId).map(User::getPhoneNumber).orElse(null);
	}

	@Transactional(readOnly = true)
	public boolean isFirstMessage(Long conversationId) {
		return messageRepository.countByConversationId(conversationId, null, null, null, null) == 1;
	}

	@Transactional(readOnly = true)
	public String getConversationType(Long conversationId) {
		return conversationRepository.findById(conversationId).map(c -> c.getType().name()).orElse(null);
	}

	@Transactional(readOnly = true)
	public List<Long> getOtherParticipantIds(Long conversationId, Long excludeUserId) {
		return conversationParticipantRepository
				.findByConversationIdAndStatus(conversationId, ConversationParticipant.ParticipantStatus.ACTIVE)
				.stream().map(p -> p.getUser().getId()).filter(id -> !id.equals(excludeUserId))
				.collect(Collectors.toList());
	}
}
