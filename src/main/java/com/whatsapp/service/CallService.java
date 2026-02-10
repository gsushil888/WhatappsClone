package com.whatsapp.service;

import com.whatsapp.dto.CallDto;
import com.whatsapp.entity.Call;
import com.whatsapp.entity.CallParticipant;
import com.whatsapp.entity.Conversation;
import com.whatsapp.entity.ErrorCode;
import com.whatsapp.entity.User;
import com.whatsapp.exception.UserException;
import com.whatsapp.repository.CallRepository;
import com.whatsapp.repository.ConversationRepository;
import com.whatsapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRepository callRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    @Transactional
    public CallDto.CallResponse initiateCall(Long userId, CallDto.InitiateCallRequest request) {
        User caller = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        Conversation conversation = null;
        if (request.getConversationId() != null) {
            conversation = conversationRepository.findById(request.getConversationId())
                    .orElseThrow(() -> new UserException(ErrorCode.CONVERSATION_NOT_FOUND));
        }

        Call call = Call.builder()
                .caller(caller)
                .conversation(conversation)
                .type(Call.CallType.valueOf(request.getCallType().toUpperCase()))
                .status(Call.CallStatus.INITIATED)
                .startedAt(LocalDateTime.now())
                .callToken(UUID.randomUUID().toString())
                .build();

        call = callRepository.save(call);
        
        // Create participants
        List<CallDto.ParticipantDto> participants = request.getParticipantIds().stream()
                .map(participantId -> {
                    User participant = userRepository.findById(participantId).orElse(null);
                    return participant != null ? CallDto.ParticipantDto.builder()
                            .userId(participant.getId())
                            .displayName(participant.getDisplayName())
                            .profilePictureUrl(participant.getProfilePictureUrl())
                            .participantStatus("CALLING")
                            .build() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return CallDto.CallResponse.builder()
                .id(call.getId())
                .callType(call.getType().name())
                .callStatus(call.getStatus().name())
                .startedAt(call.getStartedAt())
                .conversationId(conversation != null ? conversation.getId() : null)
                .callToken(call.getCallToken())
                .participants(participants)
                .build();
    }

    @Transactional
    public CallDto.CallResponse answerCall(Long userId, Long callId, CallDto.AnswerCallRequest request) {
        Call call = callRepository.findByIdAndUserId(callId, userId)
                .orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

        call.setStatus(Call.CallStatus.ANSWERED);
        call.setAnsweredAt(LocalDateTime.now());
        call = callRepository.save(call);

        return mapToCallResponse(call);
    }

    @Transactional
    public CallDto.CallResponse declineCall(Long userId, Long callId, CallDto.DeclineCallRequest request) {
        Call call = callRepository.findByIdAndUserId(callId, userId)
                .orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

        call.setStatus(Call.CallStatus.DECLINED);
        call.setEndedAt(LocalDateTime.now());
        if (request != null && request.getReason() != null) {
            call.setEndReason(request.getReason());
        }
        call = callRepository.save(call);

        return mapToCallResponse(call);
    }

    @Transactional
    public CallDto.CallResponse endCall(Long userId, Long callId) {
        Call call = callRepository.findByIdAndUserId(callId, userId)
                .orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

        call.setStatus(Call.CallStatus.ENDED);
        call.setEndedAt(LocalDateTime.now());
        
        if (call.getAnsweredAt() != null) {
            long duration = ChronoUnit.SECONDS.between(call.getAnsweredAt(), call.getEndedAt());
            call.setDurationSeconds((int) duration);
        }
        
        call = callRepository.save(call);
        return mapToCallResponse(call);
    }

    @Transactional
    public CallDto.CallResponse joinCall(Long userId, Long callId, CallDto.JoinCallRequest request) {
        Call call = callRepository.findById(callId)
                .orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

        if (!call.getStatus().equals(Call.CallStatus.ANSWERED)) {
            throw new UserException(ErrorCode.CALL_NOT_ACTIVE);
        }

        return mapToCallResponse(call);
    }

    @Transactional
    public CallDto.CallResponse leaveCall(Long userId, Long callId) {
        Call call = callRepository.findByIdAndUserId(callId, userId)
                .orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

        return mapToCallResponse(call);
    }

    @Transactional(readOnly = true)
    public CallDto.CallResponse getCallDetails(Long userId, Long callId) {
        Call call = callRepository.findByIdAndUserId(callId, userId)
                .orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

        return mapToCallResponse(call);
    }

    @Transactional
    public CallDto.CallParticipantResponse toggleMute(Long userId, Long callId, Boolean isMuted) {
        Call call = callRepository.findByIdAndUserId(callId, userId)
                .orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

        return CallDto.CallParticipantResponse.builder()
                .participantId(userId)
                .userId(userId)
                .isMuted(isMuted)
                .build();
    }

    @Transactional
    public CallDto.CallParticipantResponse toggleVideo(Long userId, Long callId, Boolean isVideoEnabled) {
        Call call = callRepository.findByIdAndUserId(callId, userId)
                .orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));

        return CallDto.CallParticipantResponse.builder()
                .participantId(userId)
                .userId(userId)
                .isVideoEnabled(isVideoEnabled)
                .build();
    }

    @Transactional(readOnly = true)
    public CallDto.CallHistoryResponse getCallHistory(Long userId, int limit, int offset, String type, String status) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        List<Call> calls = callRepository.findUserCallHistory(userId, pageable);
        
        List<CallDto.CallResponse> callResponses = calls.stream()
                .filter(call -> type == null || call.getType().name().equalsIgnoreCase(type))
                .filter(call -> status == null || call.getStatus().name().equalsIgnoreCase(status))
                .map(this::mapToCallResponse)
                .collect(Collectors.toList());
        
        return CallDto.CallHistoryResponse.builder()
                .calls(callResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public CallDto.CallStatisticsResponse getCallStatistics(Long userId, String period) {
        LocalDateTime since = calculateSinceDate(period);
        
        long totalCalls = callRepository.countUserCallsSince(userId, since);
        Long totalDuration = callRepository.getTotalCallDuration(userId, since);
        
        return CallDto.CallStatisticsResponse.builder()
                .totalCalls(totalCalls)
                .totalDurationMinutes(totalDuration != null ? totalDuration / 60 : 0L)
                .build();
    }

    @Transactional
    public void sendWebRTCSignal(Long userId, Long callId, CallDto.WebRTCSignalRequest request) {
        Call call = callRepository.findByIdAndUserId(callId, userId)
                .orElseThrow(() -> new UserException(ErrorCode.CALL_NOT_FOUND));
        
        log.info("WebRTC signal sent for call {} from user {} to participant {}", 
                callId, userId, request.getTargetParticipantId());
    }

    private CallDto.CallResponse mapToCallResponse(Call call) {
        return CallDto.CallResponse.builder()
                .id(call.getId())
                .callType(call.getType().name())
                .callStatus(call.getStatus().name())
                .startedAt(call.getStartedAt())
                .endedAt(call.getEndedAt())
                .durationSeconds(call.getDurationSeconds())
                .conversationId(call.getConversation() != null ? call.getConversation().getId() : null)
                .callToken(call.getCallToken())
                .build();
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