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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
	private final SimpMessagingTemplate messagingTemplate;

	@Transactional(readOnly = true)
	public ConversationDto.ConversationListResponse getUserConversations(Long userId, int limit, int offset,
			String filter) {
		Pageable pageable = PageRequest.of(offset / limit, limit);
		List<Conversation> conversations;

		switch (filter.toLowerCase()) {
		case "favorite":
		case "favorites":
			conversations = conversationRepository.findFavoriteConversations(userId, pageable);
			break;
		case "unread":
			conversations = conversationRepository.findUnreadConversations(userId, pageable);
			break;
		case "blocked":
			conversations = conversationRepository.findBlockedConversations(userId, pageable);
			break;
		case "archived":
			conversations = conversationRepository.findArchivedConversations(userId, pageable);
			break;
		default:
			conversations = conversationRepository.findUserConversations(userId, pageable);
		}

		if (conversations.isEmpty()) {
			long totalCount = conversationRepository.countUserConversations(userId);
			return ConversationDto.ConversationListResponse.builder().conversations(Collections.emptyList())
					.pagination(ApiResponse.PaginationInfo.builder().page(offset / limit + 1).limit(limit)
							.total((int) totalCount).hasNext(false).build())
					.build();
		}

		List<Long> convIds = conversations.stream().map(Conversation::getId).collect(Collectors.toList());

		// 1 query — viewer's participant row for every conversation
		Map<Long, ConversationParticipant> myParticipantByConvId = participantRepository
				.findByConversationIdsAndUserId(convIds, userId).stream()
				.collect(Collectors.toMap(cp -> cp.getConversation().getId(), cp -> cp, (a, b) -> a));

		// 1 query — ALL participants for every conversation (needed for INDIVIDUAL other-user lookup)
		Map<Long, List<ConversationParticipant>> allParticipantsByConvId = participantRepository
				.findByConversationIdIn(convIds).stream()
				.collect(Collectors.groupingBy(cp -> cp.getConversation().getId()));

		// 1 query — last message per conversation
		Map<Long, Message> lastMessageByConvId = messageRepository
				.findLastMessagesByConversationIds(convIds).stream()
				.collect(Collectors.toMap(m -> m.getConversation().getId(), m -> m, (a, b) -> a));

		// 1 query — unread counts per conversation
		Map<Long, Long> unreadByConvId = new HashMap<>();
		messageStatusRepository.countUnreadMessagesPerConversation(convIds, userId)
				.forEach(row -> unreadByConvId.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue()));

		// 1 query — contacts for all other-user IDs the viewer might need display names for
		Set<Long> otherUserIds = allParticipantsByConvId.values().stream()
				.flatMap(List::stream)
				.map(cp -> cp.getUser().getId())
				.filter(id -> !id.equals(userId))
				.collect(Collectors.toSet());
		Map<Long, Contact> contactByUserId = otherUserIds.isEmpty() ? Collections.emptyMap()
				: contactRepository.findByUserIdAndContactUserIdIn(userId, List.copyOf(otherUserIds)).stream()
						.collect(Collectors.toMap(c -> c.getContactUser().getId(), c -> c, (a, b) -> a));

		log.info("User {} fetched {} conversations with filter '{}'", userId, conversations.size(), filter);

		List<ConversationDto.ConversationResponse> conversationResponses = conversations.stream()
				.map(conv -> mapToConversationResponseBulk(conv, userId,
						myParticipantByConvId.get(conv.getId()),
						allParticipantsByConvId.getOrDefault(conv.getId(), Collections.emptyList()),
						lastMessageByConvId.get(conv.getId()),
						unreadByConvId.getOrDefault(conv.getId(), 0L),
						contactByUserId))
				.collect(Collectors.toList());

		long totalCount = conversationRepository.countUserConversations(userId);
		return ConversationDto.ConversationListResponse.builder().conversations(conversationResponses)
				.pagination(ApiResponse.PaginationInfo.builder().page(offset / limit + 1).limit(limit)
						.total((int) totalCount).hasNext(conversationResponses.size() == limit).build())
				.build();
	}

	private ConversationDto.ConversationResponse mapToConversationResponseBulk(Conversation conversation, Long userId,
			ConversationParticipant participant, List<ConversationParticipant> allParticipants,
			Message lastMsg, long unreadCount, Map<Long, Contact> contactByUserId) {

		ConversationDto.LastMessageDto lastMessage = null;
		if (lastMsg != null) {
			User sender = lastMsg.getSender();
			String senderName = sender != null ? resolveDisplayName(userId, sender, contactByUserId) : null;
			lastMessage = ConversationDto.LastMessageDto.builder().id(lastMsg.getId()).content(lastMsg.getContent())
					.type(lastMsg.getType() != null ? lastMsg.getType().name() : "TEXT")
					.timestamp(lastMsg.getCreatedAt())
					.sender(sender != null ? ConversationDto.MessageSenderDto.builder().id(sender.getId())
							.displayName(senderName).build() : null)
					.status(lastMsg.getStatus() != null ? lastMsg.getStatus().name() : null).build();
		}

		String profilePictureUrl = conversation.getGroupImageUrl();
		String mobileNumber = null;
		String displayTitle = conversation.getName();
		Boolean isOnline = null;
		LocalDateTime lastActiveAt = null;

		if (conversation.getType() == Conversation.ConversationType.INDIVIDUAL) {
			ConversationParticipant other = allParticipants.stream()
					.filter(p -> !p.getUser().getId().equals(userId)).findFirst().orElse(null);
			if (other != null) {
				User otherUser = other.getUser();
				profilePictureUrl = otherUser.getProfilePictureUrl();
				mobileNumber = otherUser.getPhoneNumber();
				isOnline = presenceService.getUserStatus(otherUser.getId()) == UserPresence.Status.ONLINE;
				lastActiveAt = otherUser.getLastActiveAt();
				displayTitle = resolveDisplayName(userId, otherUser, contactByUserId);
			}
		}

		boolean isRemoved = participant != null
				&& participant.getStatus() == ConversationParticipant.ParticipantStatus.REMOVED;
		String removedByName = null;
		if (isRemoved && participant.getRemovedBy() != null) {
			removedByName = resolveDisplayName(userId, participant.getRemovedBy(), contactByUserId);
		}

		return ConversationDto.ConversationResponse.builder().id(conversation.getId()).title(displayTitle)
				.type(conversation.getType().name()).lastMessage(lastMessage).unreadCount((int) unreadCount)
				.isPinned(participant != null && Boolean.TRUE.equals(participant.getIsPinned()))
				.isMuted(participant != null && Boolean.TRUE.equals(participant.getIsMuted()))
				.isArchived(participant != null && Boolean.TRUE.equals(participant.getIsArchived()))
				.isFavorite(participant != null && Boolean.TRUE.equals(participant.getIsFavorite()))
				.profileImageUrl(profilePictureUrl).mobileNumber(mobileNumber).isOnline(isOnline)
				.lastActiveAt(lastActiveAt).createdAt(conversation.getCreatedAt())
				.removedAt(isRemoved ? participant.getRemovedAt() : null).removedByName(removedByName).build();
	}

	private String resolveDisplayName(Long viewerUserId, User targetUser, Map<Long, Contact> contactByUserId) {
		Contact contact = contactByUserId.get(targetUser.getId());
		if (contact != null && contact.getDisplayName() != null && !contact.getDisplayName().trim().isEmpty()) {
			return contact.getDisplayName();
		}
		return targetUser.getPhoneNumber();
	}

	@Transactional
	public ConversationDto.ConversationResponse createConversation(Long userId,
			ConversationDto.CreateConversationRequest request) {
		User user = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		// Validate request based on conversation type
		if ("INDIVIDUAL".equalsIgnoreCase(request.getType())) {
			if (request.getParticipantId() == null) {
				throw new UserException(ErrorCode.CONV_CREATION_FAILED,
						"Participant ID is required for individual chats");
			}

			Optional<Conversation> existing = conversationRepository.findIndividualConversation(userId,
					request.getParticipantId());

			if (existing.isPresent()) {
				Conversation conv = existing.get();
				log.info("Found existing individual conversation {} between user {} and {}",
						conv.getId(), userId, request.getParticipantId());

				// Re-surface for any participant who had LEFT (deleted the chat)
				// Set clearedAt = now so they only see messages from this point forward
				List<ConversationParticipant> allParticipants =
						participantRepository.findByConversationId(conv.getId());

				for (ConversationParticipant cp : allParticipants) {
					if (cp.getStatus() == ConversationParticipant.ParticipantStatus.LEFT) {
						cp.setStatus(ConversationParticipant.ParticipantStatus.ACTIVE);
						cp.setClearedAt(LocalDateTime.now()); // fresh start — no old messages visible
						cp.setLeftAt(null);
						participantRepository.save(cp);
						log.info("Re-surfaced conversation {} for user {} with fresh message view",
								conv.getId(), cp.getUser().getId());
					}
				}

				return mapToConversationResponse(conv, userId);
			}
		} else if ("GROUP".equalsIgnoreCase(request.getType())) {
			if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
				throw new UserException(ErrorCode.CONV_CREATION_FAILED, "Participant IDs are required for group chats");
			}
		}

		Conversation conversation = Conversation.builder()
				.type(Conversation.ConversationType.valueOf(request.getType().toUpperCase())).name(request.getTitle())
				.description(request.getDescription()).groupImageUrl(request.getGroupPictureUrl()).createdBy(user)
				.build();

		conversation = conversationRepository.save(conversation);
		log.info("User {} created {} conversation {} (title='{}')",
				userId, conversation.getType(), conversation.getId(), conversation.getName());

		ConversationParticipant creatorParticipant = ConversationParticipant.builder().conversation(conversation)
				.user(user).role(ConversationParticipant.ParticipantRole.OWNER)
				.status(ConversationParticipant.ParticipantStatus.ACTIVE).isArchived(false).isFavorite(false)
				.isPinned(false).isMuted(false).build();
		participantRepository.save(creatorParticipant);

		// Handle individual chat participant
		if ("INDIVIDUAL".equalsIgnoreCase(request.getType()) && request.getParticipantId() != null) {
			User participant = userRepository.findById(request.getParticipantId())
					.orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND, "Participant not found"));

			if (!participant.getId().equals(userId)) {
				ConversationParticipant cp = ConversationParticipant.builder().conversation(conversation)
						.user(participant).role(ConversationParticipant.ParticipantRole.MEMBER)
						.status(ConversationParticipant.ParticipantStatus.ACTIVE).isArchived(false).isFavorite(false)
						.isPinned(false).isMuted(false).build();
				participantRepository.save(cp);
			}
		}

		// Handle group chat participants
		if (request.getParticipantIds() != null) {
			for (Long participantId : request.getParticipantIds()) {
				if (!participantId.equals(userId)) {
					User participant = userRepository.findById(participantId).orElse(null);
					if (participant != null) {
						ConversationParticipant cp = ConversationParticipant.builder().conversation(conversation)
								.user(participant).role(ConversationParticipant.ParticipantRole.MEMBER)
								.status(ConversationParticipant.ParticipantStatus.ACTIVE).isArchived(false)
								.isFavorite(false).isPinned(false).isMuted(false).addedBy(user).build();
						participantRepository.save(cp);
					}
				}
			}
		}

		ConversationDto.ConversationResponse response = mapToConversationResponse(conversation, userId);

		// For GROUP: push immediately to all added participants
		// For INDIVIDUAL: User B sees the conversation only when first message arrives
		if (conversation.getType() == Conversation.ConversationType.GROUP) {
			List<Long> participantIds = participantRepository
					.findByConversationIdAndStatus(conversation.getId(), ConversationParticipant.ParticipantStatus.ACTIVE)
					.stream().map(p -> p.getUser().getId()).filter(id -> !id.equals(userId))
					.collect(Collectors.toList());
			log.info("Pushing new GROUP conversation {} to {} participants", conversation.getId(), participantIds.size());
			for (Long participantId : participantIds) {
				ConversationDto.ConversationResponse personalizedResponse = mapToConversationResponse(conversation, participantId);
				messagingTemplate.convertAndSendToUser(participantId.toString(), "/queue/new-conversation", personalizedResponse);
			}
		}

		return response;
	}

	@Transactional(readOnly = true)
	public ConversationDto.ConversationDetailsResponse getConversationDetails(Long userId, Long conversationId) {
		log.info("User {} fetching details of conversation {}", userId, conversationId);
		Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_NOT_FOUND));
		return mapToConversationDetailsResponse(conversation, userId);
	}

	@Transactional
	public ConversationDto.ConversationResponse updateConversation(Long userId, Long conversationId,
			ConversationDto.UpdateConversationRequest request) {
		log.info("User {} updating conversation {}", userId, conversationId);
		Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_NOT_FOUND));

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
	public ConversationDto.ConversationSettingsResponse updateConversationSettings(Long userId, Long conversationId,
			ConversationDto.UpdateConversationSettingsRequest request) {
		conversationRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_NOT_FOUND));
		return ConversationDto.ConversationSettingsResponse.builder().conversationId(conversationId)
				.updatedAt(LocalDateTime.now()).build();
	}

	@Transactional
	public ConversationDto.AddParticipantsResponse addParticipants(Long userId, Long conversationId,
			ConversationDto.AddParticipantsRequest request) {
		log.info("User {} adding {} participants to conversation {}", userId, request.getUserIds().size(), conversationId);
		Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_NOT_FOUND));

		User adder = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		List<ConversationDto.ParticipantDto> addedParticipantDtos = new java.util.ArrayList<>();
		List<Long> readdedIds = new java.util.ArrayList<>();
		List<Long> newIds = new java.util.ArrayList<>();

		for (Long participantId : request.getUserIds()) {
			User participant = userRepository.findById(participantId).orElse(null);
			if (participant == null) continue;

			ConversationParticipant cp = participantRepository
					.findByConversationIdAndUserId(conversationId, participantId)
					.orElse(ConversationParticipant.builder().conversation(conversation)
							.user(participant).isArchived(false).isFavorite(false)
							.isPinned(false).isMuted(false).build());

			boolean wasRemoved = cp.getStatus() == ConversationParticipant.ParticipantStatus.REMOVED;
			LocalDateTime previousRemovedAt = wasRemoved ? cp.getRemovedAt() : null;

			cp.setStatus(ConversationParticipant.ParticipantStatus.ACTIVE);
			cp.setRole(ConversationParticipant.ParticipantRole.MEMBER);
			cp.setAddedBy(adder);
			cp.setRemovedBy(null);
			cp.setRemovedAt(null);
			cp.setLeftAt(null);
			cp.setJoinedAt(LocalDateTime.now());
			if (wasRemoved) {
				cp.setGapStart(previousRemovedAt);
				cp.setReaddedAt(LocalDateTime.now());
			} else {
				cp.setClearedAt(LocalDateTime.now());
				cp.setReaddedAt(null);
				cp.setGapStart(null);
			}
			participantRepository.save(cp);

			addedParticipantDtos.add(mapToParticipantDto(cp, userId));
			if (wasRemoved) readdedIds.add(participantId);
			else newIds.add(participantId);
		}

		// Notify re-added users — keep existing conversation, just re-enable input
		for (Long readdedId : readdedIds) {
			messagingTemplate.convertAndSendToUser(readdedId.toString(), "/queue/conversation-update",
					java.util.Map.of(
							"conversationId", conversationId,
							"event", "PARTICIPANT_READDED",
							"addedByUserId", userId,
							"addedByName", getDisplayName(readdedId, adder)));
		}

		// Notify brand-new users — push full conversation so it appears in their list
		for (Long newId : newIds) {
			messagingTemplate.convertAndSendToUser(newId.toString(), "/queue/new-conversation",
					mapToConversationResponse(conversation, newId));
		}

		// Notify existing members about the new participants
		List<Long> existingMemberIds = participantRepository
				.findByConversationIdAndStatus(conversationId, ConversationParticipant.ParticipantStatus.ACTIVE)
				.stream().map(p -> p.getUser().getId())
				.filter(id -> !id.equals(userId) && !request.getUserIds().contains(id))
				.collect(Collectors.toList());
		for (Long memberId : existingMemberIds) {
			messagingTemplate.convertAndSendToUser(memberId.toString(), "/queue/conversation-update",
					java.util.Map.of(
							"conversationId", conversationId,
							"event", "PARTICIPANT_ADDED",
							"addedParticipants", addedParticipantDtos,
							"addedByUserId", userId));
		}

		return ConversationDto.AddParticipantsResponse.builder().addedParticipants(addedParticipantDtos).build();
	}

	@Transactional
	public void removeParticipant(Long userId, Long conversationId, Long participantId) {
		log.info("User {} removing participant {} from conversation {}", userId, participantId, conversationId);
		User remover = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, participantId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		LocalDateTime removedAt = LocalDateTime.now();
		participant.setStatus(ConversationParticipant.ParticipantStatus.REMOVED);
		participant.setRemovedBy(remover);
		participant.setRemovedAt(removedAt);
		participantRepository.save(participant);

		String removerName = getDisplayName(participantId, remover);

		// Notify removed user
		messagingTemplate.convertAndSendToUser(participantId.toString(), "/queue/conversation-update",
				java.util.Map.of(
						"conversationId", conversationId,
						"event", "PARTICIPANT_REMOVED",
						"removedUserId", participantId,
						"removedByUserId", userId,
						"removedByName", removerName,
						"removedAt", participant.getRemovedAt().toString()));

		// Notify remaining active members
		List<Long> remainingIds = participantRepository
				.findByConversationIdAndStatus(conversationId, ConversationParticipant.ParticipantStatus.ACTIVE)
				.stream().map(p -> p.getUser().getId())
				.filter(id -> !id.equals(userId) && !id.equals(participantId))
				.collect(Collectors.toList());
		for (Long memberId : remainingIds) {
			messagingTemplate.convertAndSendToUser(memberId.toString(), "/queue/conversation-update",
					java.util.Map.of(
							"conversationId", conversationId,
							"event", "PARTICIPANT_REMOVED",
							"removedUserId", participantId,
							"removedByUserId", userId,
							"removedByName", getDisplayName(memberId, remover),
							"removedAt", participant.getRemovedAt().toString()));
		}
	}

	@Transactional
	public void leaveConversation(Long userId, Long conversationId) {
		log.info("User {} leaving conversation {}", userId, conversationId);
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setStatus(ConversationParticipant.ParticipantStatus.LEFT);
		participant.setLeftAt(LocalDateTime.now());
		participantRepository.save(participant);

		// Notify remaining active members in real time
		User leavingUser = userRepository.findById(userId).orElse(null);
		List<Long> remainingIds = participantRepository
				.findByConversationIdAndStatus(conversationId, ConversationParticipant.ParticipantStatus.ACTIVE)
				.stream().map(p -> p.getUser().getId()).collect(Collectors.toList());
		for (Long memberId : remainingIds) {
			String leavingUserName = leavingUser != null ? getDisplayName(memberId, leavingUser) : userId.toString();
			messagingTemplate.convertAndSendToUser(memberId.toString(), "/queue/conversation-update",
					java.util.Map.of(
							"conversationId", conversationId,
							"event", "PARTICIPANT_LEFT",
							"leftUserId", userId,
							"leftUserName", leavingUserName,
							"leftAt", participant.getLeftAt().toString()));
		}
	}

	@Transactional
	public ConversationDto.MuteConversationResponse muteConversation(Long userId, Long conversationId,
			ConversationDto.MuteConversationRequest request) {
		log.info("User {} muting conversation {} for {} seconds", userId, conversationId, request.getDuration());
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));

		LocalDateTime muteUntil = request.getDuration() != null ? LocalDateTime.now().plusSeconds(request.getDuration())
				: null;
		participant.setIsMuted(true);
		participant.setMuteUntil(muteUntil);
		participantRepository.save(participant);

		return ConversationDto.MuteConversationResponse.builder().isMuted(true).muteUntil(muteUntil).build();
	}

	@Transactional
	public void unmuteConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsMuted(false);
		participant.setMuteUntil(null);
		participantRepository.save(participant);
	}

	@Transactional
	public void archiveConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsArchived(true);
		participantRepository.save(participant);
	}

	@Transactional
	public void unarchiveConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsArchived(false);
		participantRepository.save(participant);
	}

	@Transactional
	public void pinConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsPinned(true);
		participantRepository.save(participant);
	}

	@Transactional
	public void unpinConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsPinned(false);
		participantRepository.save(participant);
	}

	@Transactional
	public void favoriteConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsFavorite(true);
		participantRepository.save(participant);
	}

	@Transactional
	public void unfavoriteConversation(Long userId, Long conversationId) {
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));
		participant.setIsFavorite(false);
		participantRepository.save(participant);
	}

	@Transactional
	public ConversationDto.ClearConversationResponse clearConversation(Long userId, Long conversationId) {
		log.info("User {} clearing conversation {}", userId, conversationId);
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));

		participant.setClearedAt(LocalDateTime.now());
		participantRepository.save(participant);

		return ConversationDto.ClearConversationResponse.builder()
				.conversationId(conversationId)
				.clearedMessagesCount(0)
				.clearedAt(participant.getClearedAt())
				.build();
	}

	@Transactional
	public ConversationDto.DeleteConversationResponse deleteConversation(Long userId, Long conversationId) {
		log.info("User {} deleting conversation {}", userId, conversationId);
		ConversationParticipant participant = participantRepository
				.findByConversationIdAndUserId(conversationId, userId)
				.orElseThrow(() -> new ConversationException(ErrorCode.CONV_PARTICIPANT_NOT_FOUND));

		LocalDateTime now = LocalDateTime.now();
		// LEFT status = user deleted the chat (re-surfaceable when other user messages)
		// clearedAt = timestamp used as the message visibility cutoff on re-surface
		participant.setStatus(ConversationParticipant.ParticipantStatus.LEFT);
		participant.setLeftAt(now);
		participant.setClearedAt(now);
		participantRepository.save(participant);

		return ConversationDto.DeleteConversationResponse.builder()
				.conversationId(conversationId)
				.deletedMessagesCount(0)
				.deletedAt(now)
				.build();
	}

	private ConversationDto.ConversationResponse mapToConversationResponse(Conversation conversation, Long userId) {
		ConversationParticipant participant = null;
		try {
			participant = participantRepository.findByConversationIdAndUserId(conversation.getId(), userId)
					.orElse(null);
		} catch (Exception e) {
			log.error("Error fetching participant for conversation {}: {}", conversation.getId(), e.getMessage());
		}

		ConversationDto.LastMessageDto lastMessage = null;
		try {
			LocalDateTime clearedAt = participant != null ? participant.getClearedAt() : null;
			LocalDateTime removedAt = participant != null
					&& participant.getStatus() == ConversationParticipant.ParticipantStatus.REMOVED
					? participant.getRemovedAt() : null;
			LocalDateTime gapStart = participant != null ? participant.getGapStart() : null;
			LocalDateTime readdedAt = participant != null ? participant.getReaddedAt() : null;
			List<Message> lastMessages = messageRepository.findLastMessageByConversationId(conversation.getId(),
					clearedAt, removedAt, gapStart, readdedAt, PageRequest.of(0, 1));
			if (!lastMessages.isEmpty()) {
				Message msg = lastMessages.get(0);
				User sender = msg.getSender();
				String senderDisplayName = sender != null ? getDisplayName(userId, sender) : null;
				lastMessage = ConversationDto.LastMessageDto.builder().id(msg.getId()).content(msg.getContent())
						.type(msg.getType() != null ? msg.getType().name() : "TEXT").timestamp(msg.getCreatedAt())
						.sender(sender != null ? ConversationDto.MessageSenderDto.builder().id(sender.getId())
								.displayName(senderDisplayName).build() : null)
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

		String profilePictureUrl = conversation.getGroupImageUrl();
		String mobileNumber = null;
		String displayTitle = conversation.getName();
		Boolean isOnline = null;
		LocalDateTime lastActiveAt = null;

		if (conversation.getType() == Conversation.ConversationType.INDIVIDUAL) {
			try {
				ConversationParticipant otherParticipant = participantRepository
						.findByConversationId(conversation.getId())
						.stream().filter(p -> !p.getUser().getId().equals(userId)).findFirst().orElse(null);
				if (otherParticipant != null) {
					User otherUser = otherParticipant.getUser();
					profilePictureUrl = otherUser.getProfilePictureUrl();
					mobileNumber = otherUser.getPhoneNumber();

					UserPresence.Status presenceStatus = presenceService.getUserStatus(otherUser.getId());
					isOnline = presenceStatus == UserPresence.Status.ONLINE;
					lastActiveAt = otherUser.getLastActiveAt();

					displayTitle = getDisplayName(userId, otherUser);
				}
			} catch (Exception e) {
				log.error("Error fetching participant profile picture for conversation {}: {}", conversation.getId(),
						e.getMessage(), e);
			}
		}

		boolean isRemoved = participant != null && participant.getStatus() == ConversationParticipant.ParticipantStatus.REMOVED;
		String removedByName = null;
		if (isRemoved && participant.getRemovedBy() != null) {
			removedByName = getDisplayName(userId, participant.getRemovedBy());
		}

		return ConversationDto.ConversationResponse.builder().id(conversation.getId()).title(displayTitle)
				.type(conversation.getType().name()).lastMessage(lastMessage).unreadCount((int) unreadCount)
				.isPinned(participant != null && Boolean.TRUE.equals(participant.getIsPinned()))
				.isMuted(participant != null && Boolean.TRUE.equals(participant.getIsMuted()))
				.isArchived(participant != null && Boolean.TRUE.equals(participant.getIsArchived()))
				.isFavorite(participant != null && Boolean.TRUE.equals(participant.getIsFavorite()))
				.profileImageUrl(profilePictureUrl).mobileNumber(mobileNumber).isOnline(isOnline)
				.lastActiveAt(lastActiveAt).createdAt(conversation.getCreatedAt())
				.removedAt(isRemoved ? participant.getRemovedAt() : null)
				.removedByName(removedByName)
				.build();
	}

	private ConversationDto.ConversationDetailsResponse mapToConversationDetailsResponse(Conversation conversation,
			Long userId) {
		List<ConversationParticipant> participants = participantRepository
				.findByConversationIdAndStatus(conversation.getId(), ConversationParticipant.ParticipantStatus.ACTIVE);

		// For individual conversations, exclude current user from participants
		// list
		if (conversation.getType() == Conversation.ConversationType.INDIVIDUAL) {
			participants = participants.stream().filter(p -> !p.getUser().getId().equals(userId))
					.collect(Collectors.toList());
		}

		ConversationParticipant currentUserParticipant = participantRepository
				.findByConversationIdAndUserId(conversation.getId(), userId).orElse(null);

		ConversationDto.ConversationSettingsDto settings = ConversationDto.ConversationSettingsDto.builder()
				.isMuted(currentUserParticipant != null && Boolean.TRUE.equals(currentUserParticipant.getIsMuted()))
				.isPinned(currentUserParticipant != null && Boolean.TRUE.equals(currentUserParticipant.getIsPinned()))
				.isArchived(
						currentUserParticipant != null && Boolean.TRUE.equals(currentUserParticipant.getIsArchived()))
				.isFavorite(
						currentUserParticipant != null && Boolean.TRUE.equals(currentUserParticipant.getIsFavorite()))
				.muteUntil(currentUserParticipant != null ? currentUserParticipant.getMuteUntil() : null).build();

		String mobileNumber = null;
		if (conversation.getType() == Conversation.ConversationType.INDIVIDUAL) {
			mobileNumber = participants.stream().filter(p -> p.getUser() != null && !p.getUser().getId().equals(userId))
					.findFirst().map(p -> p.getUser().getPhoneNumber()).orElse(null);
		}

		Integer participantCount = conversation.getType() == Conversation.ConversationType.GROUP ? participants.size()
				: null;

		List<ConversationDto.MediaDto> mediaList = messageRepository
				.findMediaMessages(conversation.getId(), PageRequest.of(0, 50)).stream()
				.filter(msg -> msg.getAttachments() != null && !msg.getAttachments().isEmpty()).map(msg -> {
					List<MessageAttachment> sortedAtts = msg.getAttachments().stream()
							.sorted(java.util.Comparator.comparingInt(
									a -> (a.getSortOrder() != null ? a.getSortOrder() : 0)))
							.collect(Collectors.toList());
					MessageAttachment first = sortedAtts.get(0);
					List<ConversationDto.MediaItemDto> items = sortedAtts.stream()
							.map(a -> ConversationDto.MediaItemDto.builder().url(a.getFileUrl())
									.thumbnailUrl(a.getThumbnailUrl()).fileName(a.getFileName())
									.fileSize(a.getFileSize()).mimeType(a.getMimeType()).width(a.getWidth())
									.height(a.getHeight()).duration(a.getDuration())
									.type(a.getType() != null ? a.getType().name() : null).build())
							.collect(Collectors.toList());
					return ConversationDto.MediaDto.builder().messageId(msg.getId())
							.type(msg.getType() != null ? msg.getType().name() : null).url(first.getFileUrl())
							.thumbnailUrl(first.getThumbnailUrl()).fileName(first.getFileName())
							.fileSize(first.getFileSize()).timestamp(msg.getCreatedAt())
							.sender(msg.getSender() != null
									? ConversationDto.MessageSenderDto.builder().id(msg.getSender().getId())
												.displayName(getDisplayName(userId, msg.getSender())).build()
									: null)
							.items(items).build();
				}).collect(Collectors.toList());

		return ConversationDto.ConversationDetailsResponse.builder().id(conversation.getId())
				.title(conversation.getName()).type(conversation.getType().name())
				.description(conversation.getDescription()).groupPictureUrl(conversation.getGroupImageUrl())
				.createdAt(conversation.getCreatedAt())
				.createdBy(ConversationDto.UserDto.builder().id(conversation.getCreatedBy().getId())
						.displayName(conversation.getCreatedBy().getDisplayName())
						.profilePictureUrl(conversation.getCreatedBy().getProfilePictureUrl()).build())
				.participants(
						participants.stream().map(p -> mapToParticipantDto(p, userId)).collect(Collectors.toList()))
				.settings(settings).mobileNumber(mobileNumber).participantCount(participantCount).media(mediaList)
				.build();
	}

	private ConversationDto.ParticipantDto mapToParticipantDto(ConversationParticipant participant,
			Long currentUserId) {
		User user = participant.getUser();

		String displayName = getDisplayName(currentUserId, user);

		// Get online status from PresenceService
		UserPresence.Status presenceStatus = presenceService.getUserStatus(user.getId());
		boolean isOnline = presenceStatus == UserPresence.Status.ONLINE;

		String addedByName = null;
		if (participant.getAddedBy() != null) {
			addedByName = getDisplayName(currentUserId, participant.getAddedBy());
		}

		String removedByName = null;
		if (participant.getRemovedBy() != null) {
			removedByName = getDisplayName(currentUserId, participant.getRemovedBy());
		}

		return ConversationDto.ParticipantDto.builder().userId(user.getId()).displayName(displayName)
				.mobileNumber(user.getPhoneNumber()).profilePictureUrl(user.getProfilePictureUrl())
				.participantRole(participant.getRole().name()).isOnline(isOnline).lastActiveAt(user.getLastActiveAt())
				.joinedAt(participant.getJoinedAt()).addedByName(addedByName)
				.removedByName(removedByName).removedAt(participant.getRemovedAt()).build();
	}

	private String getDisplayName(Long currentUserId, User targetUser) {
		Optional<Contact> contact = contactRepository.findByUserIdAndContactUserId(currentUserId, targetUser.getId());
		if (contact.isPresent()) {
			String saved = contact.get().getDisplayName();
			return (saved != null && !saved.trim().isEmpty()) ? saved : targetUser.getPhoneNumber();
		}
		return targetUser.getPhoneNumber();
	}
}
