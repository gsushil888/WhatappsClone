package com.whatsapp.service;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.ConversationDto;
import com.whatsapp.entity.*;
import com.whatsapp.exception.ConversationException;
import com.whatsapp.exception.UserException;
import com.whatsapp.repository.*;
import com.whatsapp.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

	private final ConversationRepository conversationRepository;
	private final UserRepository userRepository;
	private final ConversationParticipantRepository participantRepository;
	private final MessageRepository messageRepository;
	private final MessageStatusRepository messageStatusRepository;
	private final ContactRepository contactRepository;
	private final PresenceService presenceService;

	@Transactional(readOnly = true)
	public ConversationDto.ConversationListResponse getUserConversations(
			Long userId, int limit, int offset, String filter) {
		Pageable pageable = PageRequest.of(offset / limit, limit);
		List<Conversation> conversations;

		switch (filter.toLowerCase()) {
			case "favorite" :
			case "favorites" :
				conversations = conversationRepository
						.findFavoriteConversations(userId, pageable);
				break;
			case "unread" :
				conversations = conversationRepository
						.findUnreadConversations(userId, pageable);
				break;
			case "blocked" :
				conversations = conversationRepository
						.findBlockedConversations(userId, pageable);
				break;
			case "archived" :
				conversations = conversationRepository
						.findArchivedConversations(userId, pageable);
				break;
			default :
				conversations = conversationRepository
						.findUserConversations(userId, pageable);
		}
		log.info("CONVERSATIONS=> " + conversations);
		List<ConversationDto.ConversationResponse> conversationResponses = conversations
				.stream().map(conv -> mapToConversationResponse(conv, userId))
				.collect(Collectors.toList());

		long totalCount = conversationRepository.countUserConversations(userId);

		return ConversationDto.ConversationListResponse.builder()
				.conversations(conversationResponses)
				.pagination(ApiResponse.PaginationInfo.builder()
						.page(offset / limit + 1).limit(limit)
						.total((int) totalCount)
						.hasNext(conversationResponses.size() == limit).build())
				.build();
	}

	@Transactional
	public ConversationDto.ConversationResponse createConversation(Long userId,
			ConversationDto.CreateConversationRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		// Validate request based on conversation type
		if ("INDIVIDUAL".equalsIgnoreCase(request.getType())) {
			if (request.getParticipantId() == null) {
				throw new UserException(ErrorCode.CONV_CREATION_FAILED,
						"Participant ID is required for individual chats");
			}
			// Check if conversation already exists
			Optional<Conversation> existing = conversationRepository
					.findIndividualConversation(userId,
							request.getParticipantId());
			if (existing.isPresent()) {
				return mapToConversationResponse(existing.get(), userId);
			}
		} else if ("GROUP".equalsIgnoreCase(request.getType())) {
			if (request.getParticipantIds() == null
					|| request.getParticipantIds().isEmpty()) {
				throw new UserException(ErrorCode.CONV_CREATION_FAILED,
						"Participant IDs are required for group chats");
			}
		}

		Conversation conversation = Conversation.builder()
				.type(Conversation.ConversationType
						.valueOf(request.getType().toUpperCase()))
				.name(request.getTitle()).description(request.getDescription())
				.groupImageUrl(request.getGroupPictureUrl()).createdBy(user)
				.build();

		conversation = conversationRepository.save(conversation);

		ConversationParticipant creatorParticipant = ConversationParticipant
				.builder().conversation(conversation).user(user)
				.role(ConversationParticipant.ParticipantRole.OWNER)
				.status(ConversationParticipant.ParticipantStatus.ACTIVE)
				.isArchived(false).isFavorite(false).isPinned(false)
				.isMuted(false).build();
		participantRepository.save(creatorParticipant);

		// Handle individual chat participant
		if ("INDIVIDUAL".equalsIgnoreCase(request.getType())
				&& request.getParticipantId() != null) {
			User participant = userRepository
					.findById(request.getParticipantId()).orElseThrow(
							() -> new UserException(ErrorCode.USER_NOT_FOUND,
									"Participant not found"));

			if (!participant.getId().equals(userId)) {
				ConversationParticipant cp = ConversationParticipant.builder()
						.conversation(conversation).user(participant)
						.role(ConversationParticipant.ParticipantRole.MEMBER)
						.status(ConversationParticipant.ParticipantStatus.ACTIVE)
						.isArchived(false).isFavorite(false).isPinned(false)
						.isMuted(false).build();
				participantRepository.save(cp);
			}
		}

		// Handle group chat participants
		if (request.getParticipantIds() != null) {
			for (Long participantId : request.getParticipantIds()) {
				if (!participantId.equals(userId)) {
					User participant = userRepository.findById(participantId)
							.orElse(null);
					if (participant != null) {
						ConversationParticipant cp = ConversationParticipant
								.builder().conversation(conversation)
								.user(participant)
								.role(ConversationParticipant.ParticipantRole.MEMBER)
								.status(ConversationParticipant.ParticipantStatus.ACTIVE)
								.isArchived(false).isFavorite(false)
								.isPinned(false).isMuted(false).build();
						participantRepository.save(cp);
					}
				}
			}
		}

		return mapToConversationResponse(conversation, userId);
	}

	@Transactional(readOnly = true)
	public ConversationDto.ConversationDetailsResponse getConversationDetails(
			Long userId, Long conversationId) {
		Conversation conversation = conversationRepository
				.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_NOT_FOUND));
		return mapToConversationDetailsResponse(conversation, userId);
	}

	@Transactional
	public ConversationDto.ConversationResponse updateConversation(Long userId,
			Long conversationId,
			ConversationDto.UpdateConversationRequest request) {
		Conversation conversation = conversationRepository
				.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_NOT_FOUND));

		if (request.getTitle() != null)
			conversation.setName(request.getTitle());
		if (request.getDescription() != null)
			conversation.setDescription(request.getDescription());
		if (request.getGroupPictureUrl() != null)
			conversation.setGroupImageUrl(request.getGroupPictureUrl());

		conversation = conversationRepository.save(conversation);
		return mapToConversationResponse(conversation, userId);
	}

	@Transactional
	public ConversationDto.ConversationSettingsResponse updateConversationSettings(
			Long userId, Long conversationId,
			ConversationDto.UpdateConversationSettingsRequest request) {
		conversationRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_NOT_FOUND));
		return ConversationDto.ConversationSettingsResponse.builder()
				.conversationId(conversationId).updatedAt(LocalDateTime.now())
				.build();
	}

	@Transactional
	public ConversationDto.AddParticipantsResponse addParticipants(Long userId,
			Long conversationId,
			ConversationDto.AddParticipantsRequest request) {
		Conversation conversation = conversationRepository
				.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_NOT_FOUND));

		List<ConversationDto.ParticipantDto> addedParticipants = request
				.getUserIds().stream().map(participantId -> {
					User participant = userRepository.findById(participantId)
							.orElse(null);
					if (participant != null) {
						ConversationParticipant cp = ConversationParticipant
								.builder().conversation(conversation)
								.user(participant)
								.role(ConversationParticipant.ParticipantRole.MEMBER)
								.status(ConversationParticipant.ParticipantStatus.ACTIVE)
								.isArchived(false).isFavorite(false)
								.isPinned(false).isMuted(false).build();
						participantRepository.save(cp);
						return mapToParticipantDto(cp, userId);
					}
					return null;
				}).filter(p -> p != null).collect(Collectors.toList());

		return ConversationDto.AddParticipantsResponse.builder()
				.addedParticipants(addedParticipants).build();
	}

	@Transactional
	public void removeParticipant(Long userId, Long conversationId,
			Long participantId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, participantId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant
				.setStatus(ConversationParticipant.ParticipantStatus.REMOVED);
		participantRepository.save(participant);
	}

	@Transactional
	public void leaveConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setStatus(ConversationParticipant.ParticipantStatus.LEFT);
		participantRepository.save(participant);
	}

	@Transactional
	public ConversationDto.MuteConversationResponse muteConversation(
			Long userId, Long conversationId,
			ConversationDto.MuteConversationRequest request) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_PARTICIPANT_NOT_FOUND));

		LocalDateTime muteUntil = request.getDuration() != null
				? LocalDateTime.now().plusSeconds(request.getDuration())
				: null;
		participant.setIsMuted(true);
		participant.setMuteUntil(muteUntil);
		participantRepository.save(participant);

		return ConversationDto.MuteConversationResponse.builder().isMuted(true)
				.muteUntil(muteUntil).build();
	}

	@Transactional
	public void unmuteConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsMuted(false);
		participant.setMuteUntil(null);
		participantRepository.save(participant);
	}

	@Transactional
	public void archiveConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsArchived(true);
		participantRepository.save(participant);
	}

	@Transactional
	public void unarchiveConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsArchived(false);
		participantRepository.save(participant);
	}

	@Transactional
	public void pinConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsPinned(true);
		participantRepository.save(participant);
	}

	@Transactional
	public void unpinConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsPinned(false);
		participantRepository.save(participant);
	}

	@Transactional
	public void favoriteConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsFavorite(true);
		participantRepository.save(participant);
	}

	@Transactional
	public void unfavoriteConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsFavorite(false);
		participantRepository.save(participant);
	}

	@Transactional
	public ConversationDto.DeleteConversationResponse deleteConversation(
			Long userId, Long conversationId) {
		Conversation conversation = conversationRepository
				.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(
						ErrorCode.CONV_NOT_FOUND));

		// Delete all messages in the conversation
		List<Message> messages = messageRepository
				.findByConversationId(conversationId);
		int deletedMessagesCount = messages.size();

		// Delete message statuses first
		messages.forEach(
				msg -> messageStatusRepository.deleteByMessageId(msg.getId()));

		// Delete messages
		messageRepository.deleteByConversationId(conversationId);

		// Delete all participants
		participantRepository.deleteByConversationId(conversationId);

		// Delete conversation
		conversationRepository.delete(conversation);

		return ConversationDto.DeleteConversationResponse.builder()
				.conversationId(conversationId)
				.deletedMessagesCount(deletedMessagesCount)
				.deletedAt(LocalDateTime.now()).build();
	}

	private ConversationDto.ConversationResponse mapToConversationResponse(
			Conversation conversation, Long userId) {
		ConversationDto.LastMessageDto lastMessage = null;

		try {
			List<Message> lastMessages = messageRepository
					.findLastMessageByConversationId(conversation.getId(),
							PageRequest.of(0, 1));
			if (!lastMessages.isEmpty()) {
				Message msg = lastMessages.get(0);
				User sender = msg.getSender();
				String senderDisplayName = sender != null
						? getDisplayName(userId, sender)
						: null;
				lastMessage = ConversationDto.LastMessageDto.builder()
						.id(msg.getId()).content(msg.getContent())
						.type(msg.getType() != null
								? msg.getType().name()
								: "TEXT")
						.timestamp(msg.getCreatedAt())
						.sender(sender != null
								? ConversationDto.MessageSenderDto.builder()
										.id(sender.getId())
										.displayName(senderDisplayName).build()
								: null)
						.status(msg.getStatus() != null
								? msg.getStatus().name()
								: null)
						.build();
			}
		} catch (Exception e) {
			log.error("Error fetching last message for conversation {}: {}",
					conversation.getId(), e.getMessage());
		}

		long unreadCount = 0;
		try {
			unreadCount = messageStatusRepository
					.countUnreadMessages(conversation.getId(), userId);
		} catch (Exception e) {
			log.error("Error counting unread messages for conversation {}: {}",
					conversation.getId(), e.getMessage());
		}

		ConversationParticipant participant = null;
		try {
			participant = participantRepository
					.findByConversationIdAndUserId(conversation.getId(), userId)
					.orElse(null);
		} catch (Exception e) {
			log.error("Error fetching participant for conversation {}: {}",
					conversation.getId(), e.getMessage());
		}

		String profilePictureUrl = conversation.getGroupImageUrl();
		String mobileNumber = null;
		String displayTitle = conversation.getName();
		Boolean isOnline = null;
		LocalDateTime lastActiveAt = null;

		if (conversation
				.getType() == Conversation.ConversationType.INDIVIDUAL) {
			try {
				ConversationParticipant otherParticipant = participantRepository
						.findByConversationIdAndStatus(conversation.getId(),
								ConversationParticipant.ParticipantStatus.ACTIVE)
						.stream()
						.filter(p -> !p.getUser().getId().equals(userId))
						.findFirst().orElse(null);
				if (otherParticipant != null) {
					User otherUser = otherParticipant.getUser();
					profilePictureUrl = otherUser.getProfilePictureUrl();
					mobileNumber = otherUser.getPhoneNumber();

					// Get online status for individual chat
					UserPresence.Status presenceStatus = presenceService
							.getUserStatus(otherUser.getId());
					isOnline = presenceStatus == UserPresence.Status.ONLINE;
					lastActiveAt = otherUser.getLastActiveAt();

					Optional<Contact> contact = contactRepository
							.findByUserIdAndContactUserId(userId,
									otherUser.getId());
					if (contact.isPresent()) {
						displayTitle = contact.get().getDisplayName();
						log.info(
								"Contact saved - Conversation {}: userId={}, contactUserId={}, displayName={}",
								conversation.getId(), userId, otherUser.getId(),
								displayTitle);
					} else {
						displayTitle = mobileNumber;
						log.info(
								"Contact NOT saved - Conversation {}: userId={}, contactUserId={}, showing mobile {}",
								conversation.getId(), userId, otherUser.getId(),
								displayTitle);
					}
				} else {
					ConversationParticipant anyParticipant = participantRepository
							.findByConversationIdAndStatus(conversation.getId(),
									ConversationParticipant.ParticipantStatus.ACTIVE)
							.stream().findFirst().orElse(null);
					if (anyParticipant != null) {
						User anyUser = anyParticipant.getUser();
						profilePictureUrl = anyUser.getProfilePictureUrl();
						mobileNumber = anyUser.getPhoneNumber();
						Optional<Contact> anyContact = contactRepository
								.findByUserIdAndContactUserId(userId,
										anyUser.getId());
						displayTitle = anyContact.isPresent()
								? anyContact.get().getDisplayName()
								: mobileNumber;
					}
				}
			} catch (Exception e) {
				log.error(
						"Error fetching participant profile picture for conversation {}: {}",
						conversation.getId(), e.getMessage(), e);
			}
		}

		return ConversationDto.ConversationResponse.builder()
				.id(conversation.getId()).title(displayTitle)
				.type(conversation.getType().name()).lastMessage(lastMessage)
				.unreadCount((int) unreadCount)
				.isPinned(participant != null
						&& Boolean.TRUE.equals(participant.getIsPinned()))
				.isMuted(participant != null
						&& Boolean.TRUE.equals(participant.getIsMuted()))
				.isArchived(participant != null
						&& Boolean.TRUE.equals(participant.getIsArchived()))
				.isFavorite(participant != null
						&& Boolean.TRUE.equals(participant.getIsFavorite()))
				.profileImageUrl(profilePictureUrl).mobileNumber(mobileNumber)
				.isOnline(isOnline).lastActiveAt(lastActiveAt)
				.createdAt(conversation.getCreatedAt()).build();
	}

	private ConversationDto.ConversationDetailsResponse mapToConversationDetailsResponse(
			Conversation conversation, Long userId) {
		List<ConversationParticipant> participants = participantRepository
				.findByConversationIdAndStatus(conversation.getId(),
						ConversationParticipant.ParticipantStatus.ACTIVE);

		// For individual conversations, exclude current user from participants
		// list
		if (conversation
				.getType() == Conversation.ConversationType.INDIVIDUAL) {
			participants = participants.stream()
					.filter(p -> !p.getUser().getId().equals(userId))
					.collect(Collectors.toList());
		}

		ConversationParticipant currentUserParticipant = participantRepository
				.findByConversationIdAndUserId(conversation.getId(), userId)
				.orElse(null);

		ConversationDto.ConversationSettingsDto settings = ConversationDto.ConversationSettingsDto
				.builder()
				.isMuted(currentUserParticipant != null && Boolean.TRUE
						.equals(currentUserParticipant.getIsMuted()))
				.isPinned(currentUserParticipant != null && Boolean.TRUE
						.equals(currentUserParticipant.getIsPinned()))
				.isArchived(currentUserParticipant != null && Boolean.TRUE
						.equals(currentUserParticipant.getIsArchived()))
				.isFavorite(currentUserParticipant != null && Boolean.TRUE
						.equals(currentUserParticipant.getIsFavorite()))
				.muteUntil(currentUserParticipant != null
						? currentUserParticipant.getMuteUntil()
						: null)
				.build();

		String mobileNumber = null;
		if (conversation
				.getType() == Conversation.ConversationType.INDIVIDUAL) {
			mobileNumber = participants.stream()
					.filter(p -> p.getUser() != null
							&& !p.getUser().getId().equals(userId))
					.findFirst().map(p -> p.getUser().getPhoneNumber())
					.orElse(null);
		}

		Integer participantCount = conversation
				.getType() == Conversation.ConversationType.GROUP
						? participants.size()
						: null;

		List<ConversationDto.MediaDto> mediaList = messageRepository
				.findMediaMessages(conversation.getId(), PageRequest.of(0, 50))
				.stream().filter(msg -> msg.getAttachments() != null
						&& !msg.getAttachments().isEmpty())
				.map(msg -> {
					MessageAttachment attachment = msg.getAttachments().get(0);
					return ConversationDto.MediaDto.builder()
							.messageId(msg.getId())
							.type(msg.getType() != null
									? msg.getType().name()
									: null)
							.url(attachment.getFileUrl())
							.thumbnailUrl(attachment.getThumbnailUrl())
							.fileName(attachment.getFileName())
							.fileSize(attachment.getFileSize())
							.timestamp(msg.getCreatedAt())
							.sender(msg.getSender() != null
									? ConversationDto.MessageSenderDto.builder()
											.id(msg.getSender().getId())
											.displayName(getDisplayName(userId,
													msg.getSender()))
											.build()
									: null)
							.build();
				}).collect(Collectors.toList());

		return ConversationDto.ConversationDetailsResponse.builder()
				.id(conversation.getId()).title(conversation.getName())
				.type(conversation.getType().name())
				.description(conversation.getDescription())
				.groupPictureUrl(conversation.getGroupImageUrl())
				.createdAt(conversation.getCreatedAt())
				.createdBy(ConversationDto.UserDto.builder()
						.id(conversation.getCreatedBy().getId())
						.displayName(
								conversation.getCreatedBy().getDisplayName())
						.profilePictureUrl(conversation.getCreatedBy()
								.getProfilePictureUrl())
						.build())
				.participants(participants.stream()
						.map(p -> mapToParticipantDto(p, userId))
						.collect(Collectors.toList()))
				.settings(settings).mobileNumber(mobileNumber)
				.participantCount(participantCount).media(mediaList).build();
	}

	private ConversationDto.ParticipantDto mapToParticipantDto(
			ConversationParticipant participant, Long currentUserId) {
		User user = participant.getUser();

		String displayName = user.getDisplayName();
		Optional<Contact> contact = contactRepository
				.findByUserIdAndContactUserId(currentUserId, user.getId());
		if (contact.isPresent()) {
			displayName = contact.get().getDisplayName();
		}

		// Get online status from PresenceService
		UserPresence.Status presenceStatus = presenceService
				.getUserStatus(user.getId());
		boolean isOnline = presenceStatus == UserPresence.Status.ONLINE;

		return ConversationDto.ParticipantDto.builder().userId(user.getId())
				.displayName(displayName).mobileNumber(user.getPhoneNumber())
				.profilePictureUrl(user.getProfilePictureUrl())
				.participantRole(participant.getRole().name())
				.isOnline(isOnline).lastActiveAt(user.getLastActiveAt())
				.joinedAt(participant.getJoinedAt()).build();
	}

	private String getDisplayName(Long currentUserId, User targetUser) {
		Optional<Contact> contact = contactRepository
				.findByUserIdAndContactUserId(currentUserId,
						targetUser.getId());
		if (contact.isPresent()) {
			return contact.get().getDisplayName();
		}
		String userDisplayName = targetUser.getDisplayName();
		return (userDisplayName != null && !userDisplayName.trim().isEmpty())
				? userDisplayName
				: targetUser.getPhoneNumber();
	}
}
