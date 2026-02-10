package com.whatsapp.service;

import com.whatsapp.dto.ApiResponse;
import com.whatsapp.dto.ConversationDto;
import com.whatsapp.entity.*;
import com.whatsapp.exception.ConversationException;
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

	@Transactional(readOnly = true)
	public ConversationDto.ConversationListResponse getUserConversations(Long userId, int limit, int offset) {
		Pageable pageable = PageRequest.of(offset / limit, limit);
		List<Conversation> conversations = conversationRepository.findUserConversations(userId, pageable);

		List<ConversationDto.ConversationResponse> conversationResponses = conversations.stream()
				.map(conv -> mapToConversationResponse(conv, userId)).collect(Collectors.toList());

		long totalCount = conversationRepository.countUserConversations(userId);

		return ConversationDto.ConversationListResponse.builder().conversations(conversationResponses)
				.pagination(ApiResponse.PaginationInfo.builder().page(offset / limit + 1).limit(limit)
						.total((int) totalCount).hasNext(conversationResponses.size() == limit).build())
				.build();
	}

	@Transactional
	public ConversationDto.ConversationResponse createConversation(Long userId,
			ConversationDto.CreateConversationRequest request) {
		User user = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		Conversation conversation = Conversation.builder()
				.type(Conversation.ConversationType.valueOf(request.getType().toUpperCase())).name(request.getTitle())
				.description(request.getDescription()).groupImageUrl(request.getGroupPictureUrl()).createdBy(user)
				.build();

		conversation = conversationRepository.save(conversation);

		// Add creator as participant
		ConversationParticipant creatorParticipant = ConversationParticipant.builder().conversation(conversation)
				.user(user).role(ConversationParticipant.ParticipantRole.OWNER)
				.status(ConversationParticipant.ParticipantStatus.ACTIVE).build();
		participantRepository.save(creatorParticipant);

		// Add other participants
		if (request.getParticipantIds() != null) {
			for (Long participantId : request.getParticipantIds()) {
				if (!participantId.equals(userId)) {
					User participant = userRepository.findById(participantId).orElse(null);
					if (participant != null) {
						ConversationParticipant cp = ConversationParticipant.builder().conversation(conversation)
								.user(participant).role(ConversationParticipant.ParticipantRole.MEMBER)
								.status(ConversationParticipant.ParticipantStatus.ACTIVE).build();
						participantRepository.save(cp);
					}
				}
			}
		}

		return mapToConversationResponse(conversation, userId);
	}

	@Transactional(readOnly = true)
	public ConversationDto.ConversationDetailsResponse getConversationDetails(Long userId, Long conversationId) {
		Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_NOT_FOUND));

		return mapToConversationDetailsResponse(conversation);
	}

	@Transactional
	public ConversationDto.ConversationResponse updateConversation(Long userId, Long conversationId,
			ConversationDto.UpdateConversationRequest request) {
		Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_NOT_FOUND));

		if (request.getTitle() != null) {
			conversation.setName(request.getTitle());
		}
		if (request.getDescription() != null) {
			conversation.setDescription(request.getDescription());
		}
		if (request.getGroupPictureUrl() != null) {
			conversation.setGroupImageUrl(request.getGroupPictureUrl());
		}

		conversation = conversationRepository.save(conversation);
		return mapToConversationResponse(conversation, userId);
	}

	@Transactional
	public ConversationDto.ConversationSettingsResponse updateConversationSettings(Long userId, Long conversationId,
			ConversationDto.UpdateConversationSettingsRequest request) {
		Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_NOT_FOUND));

		// Update conversation settings logic here

		return ConversationDto.ConversationSettingsResponse.builder().conversationId(conversationId)
				.updatedAt(LocalDateTime.now()).build();
	}

	@Transactional
	public ConversationDto.AddParticipantsResponse addParticipants(Long userId, Long conversationId,
			ConversationDto.AddParticipantsRequest request) {
		Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_NOT_FOUND));

		List<ConversationDto.ParticipantDto> addedParticipants = request.getUserIds().stream().map(participantId -> {
			User participant = userRepository.findById(participantId).orElse(null);
			if (participant != null) {
				ConversationParticipant cp = ConversationParticipant.builder().conversation(conversation)
						.user(participant).role(ConversationParticipant.ParticipantRole.MEMBER)
						.status(ConversationParticipant.ParticipantStatus.ACTIVE).build();
				participantRepository.save(cp);
				return mapToParticipantDto(cp);
			}
			return null;
		}).filter(p -> p != null).collect(Collectors.toList());

		return ConversationDto.AddParticipantsResponse.builder().addedParticipants(addedParticipants).build();
	}

	@Transactional
	public void removeParticipant(Long userId, Long conversationId, Long participantId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, participantId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));

		participant.setStatus(ConversationParticipant.ParticipantStatus.REMOVED);
		participantRepository.save(participant);
	}

	@Transactional
	public void leaveConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));

		participant.setStatus(ConversationParticipant.ParticipantStatus.LEFT);
		participantRepository.save(participant);
	}

	@Transactional
	public ConversationDto.MuteConversationResponse muteConversation(Long userId, Long conversationId,
			ConversationDto.MuteConversationRequest request) {
		LocalDateTime muteUntil = request.getDuration() != null ? LocalDateTime.now().plusSeconds(request.getDuration())
				: null;

		return ConversationDto.MuteConversationResponse.builder().isMuted(true).muteUntil(muteUntil).build();
	}

	@Transactional
	public void unmuteConversation(Long userId, Long conversationId) {
		// Implementation for unmuting conversation
		log.info("Unmuting conversation {} for user {}", conversationId, userId);
	}

	@Transactional
	public void archiveConversation(Long userId, Long conversationId) {
		log.info("Archiving conversation {} for user {}", conversationId, userId);
	}

	@Transactional
	public void unarchiveConversation(Long userId, Long conversationId) {
		log.info("Unarchiving conversation {} for user {}", conversationId, userId);
	}

	@Transactional
	public void pinConversation(Long userId, Long conversationId) {
		log.info("Pinning conversation {} for user {}", conversationId, userId);
	}

	@Transactional
	public void unpinConversation(Long userId, Long conversationId) {
		log.info("Unpinning conversation {} for user {}", conversationId, userId);
	}

	private ConversationDto.ConversationResponse mapToConversationResponse(Conversation conversation, Long userId) {
		ConversationDto.LastMessageDto lastMessage = null;

		try {
			List<Message> lastMessages = messageRepository.findLastMessageByConversationId(conversation.getId(),
					PageRequest.of(0, 1));

			if (!lastMessages.isEmpty()) {
				Message msg = lastMessages.get(0);
				User sender = msg.getSender();
				lastMessage = ConversationDto.LastMessageDto.builder().id(msg.getId()).content(msg.getContent())
						.type(msg.getType() != null ? msg.getType().name() : "TEXT").timestamp(msg.getTimestamp())
						.sender(sender != null ? ConversationDto.MessageSenderDto.builder().id(sender.getId())
								.displayName(sender.getDisplayName()).build() : null)
						.status(msg.getStatus() != null ? msg.getStatus().name() : null).build();
			}
		} catch (Exception e) {
			log.error("Error fetching last message for conversation {}: {}", conversation.getId(), e.getMessage());
		}

		long unreadCount = 0;
		try {
			unreadCount = messageStatusRepository.countUnreadMessages(conversation.getId(), userId);
		} catch (Exception e) {
			log.error("Error counting unread messages for conversation {}: {}", conversation.getId(), e.getMessage());
		}

		return ConversationDto.ConversationResponse.builder().id(conversation.getId()).title(conversation.getName())
				.type(conversation.getType().name()).lastMessageAt(conversation.getLastMessageAt())
				.lastMessage(lastMessage).unreadCount((int) unreadCount)
				.groupPictureUrl(conversation.getGroupImageUrl()).createdAt(conversation.getCreatedAt()).build();
	}

	private ConversationDto.ConversationDetailsResponse mapToConversationDetailsResponse(Conversation conversation) {
		List<ConversationParticipant> participants = participantRepository
				.findByConversationIdAndStatus(conversation.getId(), ConversationParticipant.ParticipantStatus.ACTIVE);

		return ConversationDto.ConversationDetailsResponse.builder().id(conversation.getId())
				.title(conversation.getName()).type(conversation.getType().name())
				.description(conversation.getDescription()).groupPictureUrl(conversation.getGroupImageUrl())
				.createdAt(conversation.getCreatedAt())
				.createdBy(ConversationDto.UserDto.builder().id(conversation.getCreatedBy().getId())
						.displayName(conversation.getCreatedBy().getDisplayName())
						.profilePictureUrl(conversation.getCreatedBy().getProfilePictureUrl()).build())
				.participants(participants.stream().map(this::mapToParticipantDto).collect(Collectors.toList()))
				.build();
	}

	private ConversationDto.ParticipantDto mapToParticipantDto(ConversationParticipant participant) {
		User user = participant.getUser();
		return ConversationDto.ParticipantDto.builder().userId(user.getId()).displayName(user.getDisplayName())
				.profilePictureUrl(user.getProfilePictureUrl()).participantRole(participant.getRole().name())
				.isOnline(user.isOnline()).lastActiveAt(user.getLastActiveAt()).joinedAt(participant.getJoinedAt())
				.build();
	}
}