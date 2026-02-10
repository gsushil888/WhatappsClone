package com.whatsapp.service;

import com.whatsapp.dto.UserDto;
import com.whatsapp.entity.ErrorCode;
import com.whatsapp.entity.PrivacySettings;
import com.whatsapp.entity.User;
import com.whatsapp.entity.UserSession;
import com.whatsapp.exception.UserException;
import com.whatsapp.repository.UserRepository;
import com.whatsapp.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;

    @Transactional(readOnly = true)
    public UserDto.UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        return mapToUserProfileResponse(user);
    }

    @Transactional
    public UserDto.UserProfileResponse updateProfile(Long userId, UserDto.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getAbout() != null) {
            user.setAboutText(request.getAbout());
        }
        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }

        user = userRepository.save(user);
        return mapToUserProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto.UserSearchResponse> searchUsers(String query, Long excludeUserId, int limit) {
        return userRepository.searchUsers(query, excludeUserId, PageRequest.of(0, limit))
                .stream()
                .map(this::mapToUserSearchResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto.SessionResponse> getUserSessions(Long userId) {
        return userSessionRepository.findByUserIdAndStatus(userId, UserSession.SessionStatus.ACTIVE)
                .stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeSession(Long userId, String sessionId) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_PERMISSION_DENIED));

        if (!session.getUser().getId().equals(userId)) {
            throw new UserException(ErrorCode.USER_PERMISSION_DENIED);
        }

        session.setStatus(UserSession.SessionStatus.REVOKED);
        userSessionRepository.save(session);
    }

    private UserDto.UserProfileResponse mapToUserProfileResponse(User user) {
        return UserDto.UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .about(user.getAboutText())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isOnline(user.isOnline())
                .lastActiveAt(user.getLastSeenAt())
                .accountType(user.getAccountType().name())
                .privacySettings(mapToPrivacySettingsDto(user.getPrivacySettings()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserDto.UserSearchResponse mapToUserSearchResponse(User user) {
        return UserDto.UserSearchResponse.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .username(user.getUsername())
                .phoneNumber(user.getPhoneNumber())
                .profilePictureUrl(user.getProfilePictureUrl())
                .isOnline(user.isOnline())
                .build();
    }

    private UserDto.SessionResponse mapToSessionResponse(UserSession session) {
        return UserDto.SessionResponse.builder()
                .id(session.getId())
                .deviceType(session.getType().name())
                .deviceModel(session.getDeviceModel())
                .deviceOs(session.getDeviceOs())
                .deviceBrowser(session.getDeviceBrowser())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .lastActivityAt(session.getLastActivityAt())
                .createdAt(session.getCreatedAt())
                .isCurrent(false) // Would need to check against current session
                .build();
    }

    private UserDto.PrivacySettings mapToPrivacySettingsDto(PrivacySettings privacySettings) {
        if (privacySettings == null) {
            return null;
        }
        return UserDto.PrivacySettings.builder()
                .lastSeenVisibility(privacySettings.getLastSeenVisibility().name())
                .profilePhotoVisibility(privacySettings.getProfilePhotoVisibility().name())
                .statusVisibility(privacySettings.getStatusVisibility().name())
                .readReceiptsEnabled(privacySettings.getReadReceiptsEnabled())
                .groupsVisibility(privacySettings.getGroupsVisibility().name())
                .build();
    }
}