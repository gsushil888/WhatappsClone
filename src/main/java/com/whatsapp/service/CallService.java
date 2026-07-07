package com.whatsapp.service;

import com.whatsapp.dto.CallDto;
import com.whatsapp.entity.Call;
import com.whatsapp.entity.CallParticipant;
import com.whatsapp.entity.Conversation;
import com.whatsapp.entity.ErrorCode;
import com.whatsapp.entity.User;
import com.whatsapp.exception.UserException;
import com.whatsapp.repository.CallParticipantRepository;
import com.whatsapp.repository.CallRepository;
import com.whatsapp.repository.ContactRepository;
import com.whatsapp.repository.ConversationRepository;
import com.whatsapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallService {

	private final CallRepository callRepository;
	private final CallParticipantRepository callParticipantRepository;
	private final UserRepository userRepository;
	private final ConversationRepository conversationRepository;
	private final ContactRepository contactRepository;
	private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

	@Transactional
	public CallDto.CallResponse initiateCall(Long userId, CallDto.InitiateCallRequest request) {
		User caller = userRepository.findById(userId).orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

		Conversation conversation = null;
		if (request.getConversationId() != null) {
			conversation = conversationRepository.findById(request.getConversationId())
					.orElseThrow(() -> new UserException(ErrorCode.CONVERSATION_NOT_FOUND));
		}

		Call call = Call.builder().caller(caller).conversation(conversation)
				.type(Call.CallType.valueOf(request.getCallType().toUpperCase())).status(Call.CallStatus.INITIATED)
				.startedAt(LocalDateTime.now()).callToken(UUID.randomUUID().toString()).build();

		call = callRepository.save(call);

		List<CallDto.ParticipantDto> participantDtos = new ArrayList<>();
		for (Long participantId : request.getParticipantIds()) {
			User participant = userRepository.findById(participantId).orElse(null);
			if (participant == null)
				continue;
			CallParticipant cp = CallParticipant.builder().call(call).user(participant)
					.status(CallParticipant.CallParticipantStatus.INVITED).joinedAt(LocalDateTime.now()).build();
			callParticipantRepository.save(cp);
			participantDtos.add(CallDto.ParticipantDto.builder().userId(participant.getId())
					.displayName(resolveDisplayName(userId, participant))
					.profilePictureUrl(participant.getProfilePictureUrl()).participantStatus("INVITED").build());
		}

		for (Long participantId : request.getParticipantIds()) {
			// Each callee sees caller's name from their own contact list
			Map<String, Object> incomingCallPayload = new HashMap<>();
			incomingCallPayload.put("event", "INCOMING_CALL");
			incomingCallPayload.put("callId", call.getId());
			incomingCallPayload.put("callType", call.getType().name());
			incomingCallPayload.put("callToken", call.getCallToken());
			incomingCallPayload.put("callerUserId", userId);
			incomingCallPayload.put("callerName", resolveDisplayName(participantId, caller));
			incomingCallPayload.put("callerAvatar", caller.getProfilePictureUrl());
			incomingCallPayload.put("conversationId", conversation != null ? conversation.getId() : null);
			incomingCallPayload.put("isGroupCall", request.getParticipantIds().size() > 1);
			messagingTemplate.convertAndSendToUser(participantId.toString(), "/queue/incoming-call",
					incomingCallPayload);
		}

		boolean isGroupCall = request.getParticipantIds().size() > 1;
		return CallDto.CallResponse.builder().id(call.getId()).callType(call.getType().name())
				.callStatus(call.getStatus().name()).callDirection("OUTGOING")
				.displayName(isGroupCall && conversation != null ? conversation.getName()
						: (participantDtos.isEmpty() ? null : participantDtos.get(0).getDisplayName()))
				.groupImageUrl(isGroupCall && conversation != null ? conversation.getGroupImageUrl() : null)
				.callerProfilePictureUrl(caller.getProfilePictureUrl()).callerUserId(caller.getId())
				.startedAt(call.getStartedAt()).conversationId(conversation != null ? conversation.getId() : null)
				.callToken(call.getCallToken()).iceServers(getIceServers()).participants(participantDtos).build();
	}

	@Transactional
	public CallDto.CallResponse answerCall(Long userId, Long callId, CallDto.AnswerCallRequest request) {
		Call call = callRepository.findByIdAndUserId(callId, userId)
				.orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

		// Update this participant's status to JOINED
		call.getParticipants().stream().filter(p -> p.getUser().getId().equals(userId)).findFirst().ifPresent(p -> {
			p.setStatus(CallParticipant.CallParticipantStatus.JOINED);
			p.setJoinedAt(LocalDateTime.now());
			callParticipantRepository.save(p);
		});

		call.setStatus(Call.CallStatus.ANSWERED);
		call.setAnsweredAt(LocalDateTime.now());
		call = callRepository.save(call);

		// Notify caller: call answered
		Map<String, Object> answeredPayload = new HashMap<>();
		answeredPayload.put("event", "CALL_ANSWERED");
		answeredPayload.put("callId", call.getId());
		answeredPayload.put("answeredByUserId", userId);
		answeredPayload.put("callerUserId", call.getCaller().getId());
		messagingTemplate.convertAndSendToUser(call.getCaller().getId().toString(), "/queue/call-status",
				answeredPayload);

		return mapToCallResponse(call, userId);
	}

	@Transactional
	public CallDto.CallResponse declineCall(Long userId, Long callId, CallDto.DeclineCallRequest request) {
		Call call = callRepository.findByIdAndUserId(callId, userId)
				.orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

		// Update this participant's status to DECLINED
		call.getParticipants().stream().filter(p -> p.getUser().getId().equals(userId)).findFirst().ifPresent(p -> {
			p.setStatus(CallParticipant.CallParticipantStatus.DECLINED);
			callParticipantRepository.save(p);
		});

		// For 1-on-1: mark whole call DECLINED. For group: only if ALL declined
		boolean allDeclined = call.getParticipants() != null && call.getParticipants().stream()
				.allMatch(p -> p.getStatus() == CallParticipant.CallParticipantStatus.DECLINED);
		if (allDeclined) {
			call.setStatus(Call.CallStatus.DECLINED);
			call.setEndedAt(LocalDateTime.now());
			if (request != null && request.getReason() != null) {
				call.setEndReason(request.getReason());
			}
			call = callRepository.save(call);
		}

		// Notify caller
		Map<String, Object> declinedPayload = new HashMap<>();
		declinedPayload.put("event", "CALL_DECLINED");
		declinedPayload.put("callId", call.getId());
		declinedPayload.put("declinedByUserId", userId);
		declinedPayload.put("reason", request != null ? request.getReason() : null);
		messagingTemplate.convertAndSendToUser(call.getCaller().getId().toString(), "/queue/call-status",
				declinedPayload);

		return mapToCallResponse(call, userId);
	}

	@Transactional
	public CallDto.CallResponse endCall(Long userId, Long callId) {
		Call call = callRepository.findById(callId).orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

		boolean isCaller = call.getCaller().getId().equals(userId);
		boolean isParticipant = call.getParticipants() != null
				&& call.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(userId));
		if (!isCaller && !isParticipant) {
			throw new UserException(ErrorCode.CALL_NOT_FOUND);
		}

		call.setStatus(Call.CallStatus.ENDED);
		call.setEndedAt(LocalDateTime.now());

		if (call.getAnsweredAt() != null) {
			long duration = ChronoUnit.SECONDS.between(call.getAnsweredAt(), call.getEndedAt());
			call.setDurationSeconds((int) duration);
		}

		call = callRepository.save(call);

		Map<String, Object> endedPayload = new HashMap<>();
		endedPayload.put("event", "CALL_ENDED");
		endedPayload.put("callId", call.getId());
		endedPayload.put("endedByUserId", userId);
		endedPayload.put("durationSeconds", call.getDurationSeconds() != null ? call.getDurationSeconds() : 0);

		// Notify caller
		messagingTemplate.convertAndSendToUser(call.getCaller().getId().toString(), "/queue/call-status", endedPayload);

		// Notify all participants
		if (call.getParticipants() != null) {
			for (CallParticipant cp : call.getParticipants()) {
				messagingTemplate.convertAndSendToUser(cp.getUser().getId().toString(), "/queue/call-status",
						endedPayload);
			}
		}

		return mapToCallResponse(call, userId);
	}

	@Transactional
	public CallDto.CallResponse joinCall(Long userId, Long callId, CallDto.JoinCallRequest request) {
		Call call = callRepository.findById(callId).orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

		if (!call.getStatus().equals(Call.CallStatus.ANSWERED)) {
			throw new UserException(ErrorCode.CALL_NOT_ACTIVE);
		}

		// Update existing participant row to JOINED
		call.getParticipants().stream().filter(p -> p.getUser().getId().equals(userId)).findFirst().ifPresent(p -> {
			p.setStatus(CallParticipant.CallParticipantStatus.JOINED);
			p.setJoinedAt(LocalDateTime.now());
			callParticipantRepository.save(p);
		});

		// Notify all other participants that someone joined
		Map<String, Object> joinedPayload = new HashMap<>();
		joinedPayload.put("event", "PARTICIPANT_JOINED");
		joinedPayload.put("callId", callId);
		joinedPayload.put("joinedUserId", userId);

		messagingTemplate.convertAndSendToUser(call.getCaller().getId().toString(), "/queue/call-status",
				joinedPayload);
		if (call.getParticipants() != null) {
			for (CallParticipant cp : call.getParticipants()) {
				if (!cp.getUser().getId().equals(userId)) {
					messagingTemplate.convertAndSendToUser(cp.getUser().getId().toString(), "/queue/call-status",
							joinedPayload);
				}
			}
		}

		return mapToCallResponse(call, userId);
	}

	@Transactional
	public CallDto.CallResponse leaveCall(Long userId, Long callId) {
		Call call = callRepository.findByIdAndUserId(callId, userId)
				.orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

		// Mark this participant as LEFT
		call.getParticipants().stream().filter(p -> p.getUser().getId().equals(userId)).findFirst().ifPresent(p -> {
			p.setStatus(CallParticipant.CallParticipantStatus.LEFT);
			p.setLeftAt(LocalDateTime.now());
			callParticipantRepository.save(p);
		});

		// Notify remaining participants
		Map<String, Object> leftPayload = new HashMap<>();
		leftPayload.put("event", "PARTICIPANT_LEFT");
		leftPayload.put("callId", callId);
		leftPayload.put("leftUserId", userId);

		messagingTemplate.convertAndSendToUser(call.getCaller().getId().toString(), "/queue/call-status", leftPayload);
		if (call.getParticipants() != null) {
			for (CallParticipant cp : call.getParticipants()) {
				if (!cp.getUser().getId().equals(userId)) {
					messagingTemplate.convertAndSendToUser(cp.getUser().getId().toString(), "/queue/call-status",
							leftPayload);
				}
			}
		}

		return mapToCallResponse(call, userId);
	}

	@Transactional(readOnly = true)
	public CallDto.CallResponse getCallDetails(Long userId, Long callId) {
		Call call = callRepository.findByIdAndUserId(callId, userId)
				.orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));
		return mapToCallResponse(call, userId);
	}

	@Transactional
	public CallDto.CallParticipantResponse toggleMute(Long userId, Long callId, Boolean isMuted) {
		Call call = callRepository.findByIdAndUserId(callId, userId)
				.orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

		// Notify all other participants about mute state change
		Map<String, Object> mutePayload = new HashMap<>();
		mutePayload.put("event", "PARTICIPANT_MUTE_CHANGED");
		mutePayload.put("callId", callId);
		mutePayload.put("userId", userId);
		mutePayload.put("isMuted", isMuted);

		if (call.getParticipants() != null) {
			for (CallParticipant cp : call.getParticipants()) {
				if (!cp.getUser().getId().equals(userId)) {
					messagingTemplate.convertAndSendToUser(cp.getUser().getId().toString(), "/queue/call-status",
							mutePayload);
				}
			}
		}
		// Also notify caller if toggler is a participant
		if (!call.getCaller().getId().equals(userId)) {
			messagingTemplate.convertAndSendToUser(call.getCaller().getId().toString(), "/queue/call-status",
					mutePayload);
		}

		return CallDto.CallParticipantResponse.builder().participantId(userId).userId(userId).isMuted(isMuted).build();
	}

	@Transactional
	public CallDto.CallParticipantResponse toggleVideo(Long userId, Long callId, Boolean isVideoEnabled) {
		Call call = callRepository.findByIdAndUserId(callId, userId)
				.orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

		// Notify all other participants about video state change
		Map<String, Object> videoPayload = new HashMap<>();
		videoPayload.put("event", "PARTICIPANT_VIDEO_CHANGED");
		videoPayload.put("callId", callId);
		videoPayload.put("userId", userId);
		videoPayload.put("isVideoEnabled", isVideoEnabled);

		if (call.getParticipants() != null) {
			for (CallParticipant cp : call.getParticipants()) {
				if (!cp.getUser().getId().equals(userId)) {
					messagingTemplate.convertAndSendToUser(cp.getUser().getId().toString(), "/queue/call-status",
							videoPayload);
				}
			}
		}
		if (!call.getCaller().getId().equals(userId)) {
			messagingTemplate.convertAndSendToUser(call.getCaller().getId().toString(), "/queue/call-status",
					videoPayload);
		}

		return CallDto.CallParticipantResponse.builder().participantId(userId).userId(userId)
				.isVideoEnabled(isVideoEnabled).build();
	}

	@Transactional(readOnly = true)
	public CallDto.CallHistoryResponse getCallHistory(Long userId, int limit, int offset, String type, String status) {
		Pageable pageable = PageRequest.of(offset / limit, limit);
		List<Call> calls = callRepository.findUserCallHistory(userId, pageable);

		List<CallDto.CallResponse> callResponses = calls.stream()
				.filter(call -> type == null || call.getType().name().equalsIgnoreCase(type))
				.filter(call -> status == null || call.getStatus().name().equalsIgnoreCase(status))
				.map(call -> mapToCallResponse(call, userId)).collect(Collectors.toList());

		return CallDto.CallHistoryResponse
				.builder().calls(callResponses).pagination(com.whatsapp.dto.ApiResponse.PaginationInfo.builder()
						.limit(limit).total(callResponses.size()).hasNext(callResponses.size() == limit).build())
				.build();
	}

	@Transactional(readOnly = true)
	public CallDto.CallStatisticsResponse getCallStatistics(Long userId, String period) {
		LocalDateTime since = calculateSinceDate(period);
		List<Call> calls = callRepository.findUserCallsSinceList(userId, since);

		long totalCalls = calls.size();
		long videoCalls = calls.stream().filter(c -> c.getType() == Call.CallType.VIDEO).count();
		long voiceCalls = calls.stream().filter(c -> c.getType() == Call.CallType.VOICE).count();
		long missedCalls = calls.stream()
				.filter(c -> c.getStatus() == Call.CallStatus.MISSED || c.getStatus() == Call.CallStatus.DECLINED)
				.filter(c -> !c.getCaller().getId().equals(userId)) // missed = incoming not answered
				.count();
		long totalDurationSeconds = calls.stream().filter(c -> c.getDurationSeconds() != null)
				.mapToLong(Call::getDurationSeconds).sum();

		// Group by day: "2026-05-28" -> count
		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		Map<String, Long> callsByDay = calls.stream()
				.collect(Collectors.groupingBy(c -> c.getStartedAt().format(dayFmt), Collectors.counting()));

		return CallDto.CallStatisticsResponse.builder().totalCalls(totalCalls)
				.totalDurationMinutes(totalDurationSeconds / 60).videoCalls(videoCalls).voiceCalls(voiceCalls)
				.missedCalls(missedCalls).callsByDay(callsByDay).build();
	}

	@Transactional
	public void sendWebRTCSignal(Long userId, Long callId, CallDto.WebRTCSignalRequest request) {
		Call call = callRepository.findById(callId).orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));
		boolean isCaller = call.getCaller().getId().equals(userId);
		boolean isParticipant = call.getParticipants() != null
				&& call.getParticipants().stream().anyMatch(p -> p.getUser().getId().equals(userId));
		if (!isCaller && !isParticipant) {
			throw new UserException(ErrorCode.CALL_NOT_FOUND);
		}

		Map<String, Object> signal = new HashMap<>();
		signal.put("callId", callId);
		signal.put("fromUserId", userId);
		signal.put("type", request.getType());
		signal.put("data", request.getData());

		messagingTemplate.convertAndSendToUser(request.getTargetParticipantId().toString(), "/queue/call-signal",
				signal);

		log.info("WebRTC signal '{}' sent for call {} from user {} to {}", request.getType(), callId, userId,
				request.getTargetParticipantId());
	}

	// Builds full CallResponse with direction, displayName,
	// callerProfilePictureUrl, participants
	private CallDto.CallResponse mapToCallResponse(Call call, Long viewerUserId) {
		String direction = call.getCaller().getId().equals(viewerUserId) ? "OUTGOING" : "INCOMING";

		String displayName;
		String groupImageUrl = null;
		boolean isGroupCall = call.getParticipants() != null && call.getParticipants().size() > 1;

		if (isGroupCall && call.getConversation() != null && call.getConversation().getName() != null) {
			displayName = call.getConversation().getName();
			groupImageUrl = call.getConversation().getGroupImageUrl();
		} else if (direction.equals("OUTGOING") && call.getParticipants() != null
				&& !call.getParticipants().isEmpty()) {
			User other = call.getParticipants().get(0).getUser();
			displayName = resolveDisplayName(viewerUserId, other);
		} else {
			User caller = call.getCaller();
			displayName = resolveDisplayName(viewerUserId, caller);
		}

		List<CallDto.ParticipantDto> participantDtos = new ArrayList<>();
		if (call.getParticipants() != null) {
			for (CallParticipant cp : call.getParticipants()) {
				User u = cp.getUser();
				participantDtos.add(CallDto.ParticipantDto.builder().userId(u.getId())
						.displayName(resolveDisplayName(viewerUserId, u)).profilePictureUrl(u.getProfilePictureUrl())
						.participantStatus(cp.getStatus().name()).joinedAt(cp.getJoinedAt()).leftAt(cp.getLeftAt())
						.build());
			}
		}

		return CallDto.CallResponse.builder().id(call.getId()).callType(call.getType().name())
				.callStatus(call.getStatus().name()).callDirection(direction).displayName(displayName)
				.groupImageUrl(groupImageUrl).callerUserId(call.getCaller().getId())
				.callerProfilePictureUrl(call.getCaller().getProfilePictureUrl()).startedAt(call.getStartedAt())
				.endedAt(call.getEndedAt()).durationSeconds(call.getDurationSeconds())
				.conversationId(call.getConversation() != null ? call.getConversation().getId() : null)
				.callToken(call.getCallToken()).iceServers(getIceServers()).participants(participantDtos).build();
	}

	// Contact-aware name resolution:
	// 1. contact exists with displayName set → contact's displayName ("Brother")
	// 2. contact exists but displayName null → user's own displayName
	// 3. user has no displayName → user's phone number
	// 4. not a contact at all → user's phone number
	private String resolveDisplayName(Long viewerUserId, User target) {
		return contactRepository.findByUserIdAndContactUserId(viewerUserId, target.getId()).map(c -> {
			if (c.getDisplayName() != null && !c.getDisplayName().isBlank()) {
				return c.getDisplayName();
			}
			if (target.getDisplayName() != null && !target.getDisplayName().isBlank()) {
				return target.getDisplayName();
			}
			return target.getPhoneNumber();
		}).orElse(target.getPhoneNumber());
	}

	private List<CallDto.IceServer> getIceServers() {
		return List.of(CallDto.IceServer.builder()
				.urls(List.of("stun:stun.l.google.com:19302", "stun:stun1.l.google.com:19302")).build());
	}

	private LocalDateTime calculateSinceDate(String period) {
		LocalDateTime now = LocalDateTime.now();
		return switch (period.toLowerCase()) {
		case "day" -> now.minusDays(1);
		case "week" -> now.minusWeeks(1);
		case "month" -> now.minusMonths(1);
		case "year" -> now.minusYears(1);
		default -> now.minusWeeks(1);
		};
	}
}
