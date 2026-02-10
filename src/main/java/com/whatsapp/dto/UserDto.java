package com.whatsapp.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfileResponse {
        private Long id;
        private String username;
        private String displayName;
        private String phoneNumber;
        private String email;
        private String about;
        private String profilePictureUrl;
        private Boolean isOnline;
        private LocalDateTime lastActiveAt;
        private String accountType;
        private PrivacySettings privacySettings;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateProfileRequest {
        @Size(max = 100, message = "Display name cannot exceed 100 characters")
        private String displayName;

        @Size(max = 500, message = "About cannot exceed 500 characters")
        private String about;

        private String profilePictureUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePrivacyRequest {
        private String lastSeen; // EVERYONE, CONTACTS, NOBODY
        private String profilePhoto; // EVERYONE, CONTACTS, NOBODY
        private String about; // EVERYONE, CONTACTS, NOBODY
        private String status; // EVERYONE, CONTACTS, NOBODY
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PrivacySettings {
        private String lastSeenVisibility;
        private String profilePhotoVisibility;
        private String statusVisibility;
        private Boolean readReceiptsEnabled;
        private String groupsVisibility;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactResponse {
        private Long id;
        private Long contactUserId;
        private String customName;
        private String displayName;
        private String phoneNumber;
        private String profilePictureUrl;
        private Boolean isOnline;
        private Boolean isFavorite;
        private Boolean isBlocked;
        private LocalDateTime lastActiveAt;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddContactRequest {
        private Long contactUserId;
        
        @Size(max = 100, message = "Display name cannot exceed 100 characters")
        private String displayName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateContactRequest {
        @Size(max = 100, message = "Display name cannot exceed 100 characters")
        private String displayName;

        private Boolean isFavorite;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncContactsRequest {
        private List<String> phoneNumbers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SyncContactsResponse {
        private List<ContactResponse> foundContacts;
        private List<String> notFoundNumbers;
        private LocalDateTime syncedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserSearchResponse {
        private Long id;
        private String displayName;
        private String username;
        private String phoneNumber;
        private String profilePictureUrl;
        private Boolean isOnline;
        private Boolean isContact;
        private Boolean isBlocked;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactListResponse {
        private List<ContactResponse> contacts;
        private Integer totalCount;
        private PaginationInfo pagination;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaginationInfo {
        private Integer page;
        private Integer limit;
        private Boolean hasNext;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SessionResponse {
        private String id;
        private String deviceType;
        private String deviceModel;
        private String deviceOs;
        private String deviceBrowser;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime lastActivityAt;
        private LocalDateTime createdAt;
        private Boolean isCurrent;
    }
}