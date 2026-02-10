package com.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class SearchDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UniversalSearchResponse {
        private List<ContactSearchResult> contacts;
        private List<ConversationSearchResult> conversations;
        private List<MessageSearchResult> messages;
        private List<UserSearchResult> users;
        private SearchSummary summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactSearchResult {
        private Long id;
        private String type = "CONTACT";
        private ContactUser contactUser;
        private String displayName;
        private String matchedField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactUser {
        private Long id;
        private String displayName;
        private String profilePictureUrl;
        private Boolean isOnline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConversationSearchResult {
        private Long id;
        private String type = "CONVERSATION";
        private String name;
        private String profileImageUrl;
        private String conversationType;
        private LastMessage lastMessage;
        private String matchedField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LastMessage {
        private String content;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageSearchResult {
        private Long id;
        private String type = "MESSAGE";
        private String content;
        private LocalDateTime timestamp;
        private MessageConversation conversation;
        private MessageSender sender;
        private String matchedField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageConversation {
        private Long id;
        private String name;
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MessageSender {
        private Long id;
        private String displayName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserSearchResult {
        private Long id;
        private String type = "USER";
        private String username;
        private String displayName;
        private String profilePictureUrl;
        private Boolean isOnline;
        private String accountType;
        private String matchedField;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SearchSummary {
        private Integer totalContacts;
        private Integer totalConversations;
        private Integer totalMessages;
        private Integer totalUsers;
        private Boolean hasMore;
    }
}