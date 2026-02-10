# WhatsApp System Entities

## Core User Management

/**
 * User Entity - Central entity representing system users
 * Purpose: Stores user profile information, authentication details, and privacy settings
 * Importance: Core entity that all other entities reference for user identification
 * Relationships: Has many sessions, contacts, messages, stories (1:N composition)
 */
### User Entity
```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_phone", columnList = "phoneNumber"),
    @Index(name = "idx_user_username", columnList = "username"),
    @Index(name = "idx_user_status", columnList = "status"),
    @Index(name = "idx_user_online", columnList = "isOnline")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id; // Primary key - unique identifier for each user
    
    @Column(name = "username", unique = true, length = 50)
    private String username; // Unique username for login - business identifier
    
    @Column(name = "email", unique = true, length = 100)
    private String email; // Email for authentication and notifications
    
    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber; // Phone for WhatsApp-style authentication
    
    @Column(name = "password", length = 255)
    private String password; // Encrypted password for security
    
    @Column(name = "first_name", length = 50)
    private String firstName; // User's first name for personalization
    
    @Column(name = "last_name", length = 50)
    private String lastName; // User's last name for full identity
    
    @Column(name = "display_name", length = 100)
    private String displayName; // Name shown to other users
    
    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl; // Avatar image URL
    
    @Column(name = "about_text", length = 500)
    private String aboutText; // WhatsApp-style status message
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status; // Account status for moderation
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType; // Personal vs Business account
    
    @Column(name = "is_online")
    private boolean isOnline; // Real-time presence indicator
    
    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt; // Last activity timestamp
    
    @Column(name = "is_verified")
    private boolean isVerified; // Verification badge status
    
    @Column(name = "email_verified")
    private boolean emailVerified; // Email verification status
    
    @Column(name = "phone_verified")
    private boolean phoneVerified; // Phone verification status
    
    @Column(name = "two_factor_enabled")
    private boolean twoFactorEnabled; // 2FA security setting
    
    @Column(name = "privacy_settings", columnDefinition = "JSON")
    private String privacySettings; // JSON privacy preferences
    
    // HAS-A relationships (User has many sessions, contacts, etc.)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserSession> sessions; // User HAS many sessions
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Contact> contacts; // User HAS many contacts
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConversationParticipant> conversationParticipants; // User HAS many conversation participations
    
    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> sentMessages; // User HAS many sent messages
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Story> stories; // User HAS many stories
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt; // Account creation timestamp
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Last profile update timestamp
    
    public enum UserStatus { ACTIVE, INACTIVE, SUSPENDED, DELETED }
    public enum AccountType { PERSONAL, BUSINESS }
}
```

/**
 * UserSession Entity - Manages user authentication sessions across devices
 * Purpose: Tracks active user sessions for security and multi-device support
 * Importance: Critical for JWT token management and device-based authentication
 * Relationships: Belongs to User (N:1 aggregation), dependent on User existence
 */
### UserSession Entity
```java
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_session_user", columnList = "userId"),
    @Index(name = "idx_session_status", columnList = "status"),
    @Index(name = "idx_session_device", columnList = "deviceFingerprint"),
    @Index(name = "idx_session_expires", columnList = "expiresAt")
})
public class UserSession {
    @Id
    @Column(name = "id", length = 100)
    private String id; // UUID-based session identifier for security
    
    // BELONGS-TO relationship (Session belongs to User - N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Session BELONGS TO a User (mandatory dependency)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private SessionType type; // Device type for session management
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SessionStatus status; // Session lifecycle status
    
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint; // Unique device identifier
    
    @Column(name = "device_info", columnDefinition = "JSON")
    private String deviceInfo; // Device metadata for security
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress; // Client IP for security tracking
    
    @Column(name = "user_agent", length = 1000)
    private String userAgent; // Browser/app information
    
    @Column(name = "jwt_token", length = 500)
    private String jwtToken; // Access token for API authentication
    
    @Column(name = "refresh_token", length = 500)
    private String refreshToken; // Token for session renewal
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Session expiration time
    
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt; // Last API call timestamp
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt; // Session creation time
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Last session update
    
    public enum SessionType { WEB, MOBILE, DESKTOP, TABLET }
    public enum SessionStatus { ACTIVE, EXPIRED, REVOKED, TEMP }
}
```

## Contact Management

### Contact Entity
```java
@Entity
@Table(name = "contacts", indexes = {
    @Index(name = "idx_contact_user", columnList = "userId"),
    @Index(name = "idx_contact_target", columnList = "contactUserId"),
    @Index(name = "idx_contact_status", columnList = "status")
})
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_user_id", nullable = false)
    private User contactUser;
    
    @Column(name = "display_name", length = 100)
    private String displayName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ContactStatus status;
    
    @Column(name = "is_blocked")
    private Boolean isBlocked;
    
    @Column(name = "is_favorite")
    private Boolean isFavorite;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ContactStatus { ACTIVE, BLOCKED, DELETED }
}
```

## Conversation Management

### Conversation Entity
```java
@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conversation_type", columnList = "type"),
    @Index(name = "idx_conversation_created", columnList = "createdAt"),
    @Index(name = "idx_conversation_updated", columnList = "lastMessageAt")
})
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ConversationType type;
    
    @Column(name = "name", length = 100)
    private String name;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "group_image_url", length = 500)
    private String groupImageUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConversationParticipant> participants;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messages;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ConversationSetting> conversationSettings;
    
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ConversationType { INDIVIDUAL, GROUP, BROADCAST }
}
```

### ConversationSetting Entity (Normalized)
```java
@Entity
@Table(name = "conversation_settings", indexes = {
    @Index(name = "idx_setting_conversation", columnList = "conversationId"),
    @Index(name = "idx_setting_key", columnList = "settingKey"),
    @Index(name = "idx_setting_active", columnList = "isActive")
})
public class ConversationSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "setting_key", nullable = false)
    private SettingKey settingKey;
    
    @Column(name = "setting_value", length = 500)
    private String settingValue;
    
    @Column(name = "is_active")
    private Boolean isActive;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_by_user_id")
    private User setByUser;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum SettingKey {
        ONLY_ADMINS_ADD_MEMBERS,
        ONLY_ADMINS_EDIT_INFO,
        ONLY_ADMINS_SEND_MESSAGES,
        DISAPPEARING_MESSAGES_DURATION,
        ANNOUNCEMENT_ONLY,
        MUTE_NOTIFICATIONS,
        WALLPAPER_URL,
        ENCRYPTION_ENABLED,
        BACKUP_ENABLED,
        READ_RECEIPTS_ENABLED,
        TYPING_INDICATORS_ENABLED,
        MEDIA_AUTO_DOWNLOAD,
        LINK_PREVIEW_ENABLED
    }
}
```

### ConversationParticipant Entity
```java
@Entity
@Table(name = "conversation_participants", indexes = {
    @Index(name = "idx_participant_conversation", columnList = "conversationId"),
    @Index(name = "idx_participant_user", columnList = "userId"),
    @Index(name = "idx_participant_role", columnList = "role")
})
public class ConversationParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private ParticipantRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ParticipantStatus status;
    
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
    
    @Column(name = "left_at")
    private LocalDateTime leftAt;
    
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;
    
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promoted_by")
    private User promotedBy;
    
    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;
    
    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ParticipantPermission> permissions;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ParticipantRole { MEMBER, ADMIN, OWNER }
    public enum ParticipantStatus { ACTIVE, LEFT, REMOVED, BANNED }
}
```

### ParticipantPermission Entity (Normalized)
```java
@Entity
@Table(name = "participant_permissions", indexes = {
    @Index(name = "idx_permission_participant", columnList = "participantId"),
    @Index(name = "idx_permission_key", columnList = "permissionKey"),
    @Index(name = "idx_permission_granted", columnList = "isGranted")
})
public class ParticipantPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private ConversationParticipant participant;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_key", nullable = false)
    private PermissionKey permissionKey;
    
    @Column(name = "is_granted")
    private Boolean isGranted;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_user_id")
    private User grantedByUser;
    
    @Column(name = "granted_at")
    private LocalDateTime grantedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum PermissionKey {
        ADD_MEMBERS,
        REMOVE_MEMBERS,
        EDIT_GROUP_INFO,
        SEND_MESSAGES,
        DELETE_MESSAGES,
        PIN_MESSAGES,
        CHANGE_GROUP_SETTINGS,
        PROMOTE_MEMBERS,
        DEMOTE_MEMBERS,
        VIEW_ADMIN_ACTIONS,
        MANAGE_CALLS,
        SEND_MEDIA,
        SEND_DOCUMENTS,
        CREATE_POLLS
    }
}
```

### GroupAdminAction Entity
```java
@Entity
@Table(name = "group_admin_actions", indexes = {
    @Index(name = "idx_admin_action_conversation", columnList = "conversationId"),
    @Index(name = "idx_admin_action_admin", columnList = "adminUserId"),
    @Index(name = "idx_admin_action_target", columnList = "targetUserId"),
    @Index(name = "idx_admin_action_type", columnList = "actionType")
})
public class GroupAdminAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User adminUser;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private AdminActionType actionType;
    
    @Column(name = "action_details", columnDefinition = "JSON")
    private String actionDetails;
    
    @Column(name = "reason", length = 500)
    private String reason;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum AdminActionType {
        ADD_MEMBER,
        REMOVE_MEMBER,
        PROMOTE_TO_ADMIN,
        DEMOTE_FROM_ADMIN,
        UPDATE_GROUP_NAME,
        UPDATE_GROUP_DESCRIPTION,
        UPDATE_GROUP_IMAGE,
        UPDATE_GROUP_SETTINGS,
        RESTRICT_MEMBER,
        UNRESTRICT_MEMBER,
        BAN_MEMBER,
        UNBAN_MEMBER
    }
}
```mn(name = "last_read_message_id")
    private Long lastReadMessageId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;
    
    @Column(name = "permissions", columnDefinition = "JSON")
    private String permissions;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ParticipantRole { MEMBER, ADMIN, OWNER }
    public enum ParticipantStatus { ACTIVE, LEFT, REMOVED, BANNED }
}
```

## Message Management

### Message Entity
```java
@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_conversation", columnList = "conversationId"),
    @Index(name = "idx_message_sender", columnList = "senderId"),
    @Index(name = "idx_message_timestamp", columnList = "timestamp"),
    @Index(name = "idx_message_type", columnList = "type"),
    @Index(name = "idx_message_status", columnList = "status")
})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private MessageType type;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageAttachment> attachments;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_message_id")
    private Message replyToMessage;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarded_from_message_id")
    private Message forwardedFromMessage;
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageStatus> messageStatuses;
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageReaction> reactions;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeliveryStatus status;
    
    @Column(name = "is_edited")
    private Boolean isEdited;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted;
    
    @Column(name = "is_starred")
    private Boolean isStarred;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "edited_at")
    private LocalDateTime editedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum MessageType { TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION, CONTACT, STICKER }
    public enum DeliveryStatus { SENT, DELIVERED, READ }
}
```

### MessageAttachment Entity
```java
@Entity
@Table(name = "message_attachments", indexes = {
    @Index(name = "idx_attachment_message", columnList = "messageId"),
    @Index(name = "idx_attachment_type", columnList = "type")
})
public class MessageAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private AttachmentType type;
    
    @Column(name = "file_name", length = 500)
    private String fileName;
    
    @Column(name = "file_url", length = 500)
    private String fileUrl;
    
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    @Column(name = "width")
    private Integer width;
    
    @Column(name = "height")
    private Integer height;
    
    @Column(name = "duration")
    private Integer duration;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum AttachmentType { IMAGE, VIDEO, AUDIO, DOCUMENT, STICKER }
}
```

### MessageStatus Entity (Denormalized)
```java
@Entity
@Table(name = "message_statuses", indexes = {
    @Index(name = "idx_status_message", columnList = "messageId"),
    @Index(name = "idx_status_user", columnList = "userId"),
    @Index(name = "idx_status_conversation", columnList = "conversationId"),
    @Index(name = "idx_status_type", columnList = "status")
})
public class MessageStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "conversation_id") // Denormalized for performance
    private Long conversationId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private DeliveryStatus status;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum DeliveryStatus { SENT, DELIVERED, READ }
}
```

### MessageReaction Entity
```java
@Entity
@Table(name = "message_reactions", indexes = {
    @Index(name = "idx_reaction_message", columnList = "messageId"),
    @Index(name = "idx_reaction_user", columnList = "userId")
})
public class MessageReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "emoji", length = 10)
    private String emoji;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

## Call Management

### Call Entity
```java
@Entity
@Table(name = "calls", indexes = {
    @Index(name = "idx_call_conversation", columnList = "conversationId"),
    @Index(name = "idx_call_initiator", columnList = "initiatorId"),
    @Index(name = "idx_call_status", columnList = "status"),
    @Index(name = "idx_call_started", columnList = "startedAt")
})
public class Call {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private CallType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CallStatus status;
    
    @OneToMany(mappedBy = "call", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CallParticipant> participants;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @Column(name = "duration")
    private Integer duration;
    
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum CallType { VOICE, VIDEO }
    public enum CallStatus { INITIATED, RINGING, ANSWERED, ENDED, MISSED, DECLINED }
}
```

### CallParticipant Entity
```java
@Entity
@Table(name = "call_participants", indexes = {
    @Index(name = "idx_call_participant_call", columnList = "callId"),
    @Index(name = "idx_call_participant_user", columnList = "userId")
})
public class CallParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private Call call;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CallParticipantStatus status;
    
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
    
    @Column(name = "left_at")
    private LocalDateTime leftAt;
    
    @Column(name = "duration")
    private Integer duration;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum CallParticipantStatus { INVITED, JOINED, LEFT, DECLINED }
}
```

## Story Management

### Story Entity
```java
@Entity
@Table(name = "stories", indexes = {
    @Index(name = "idx_story_user", columnList = "userId"),
    @Index(name = "idx_story_created", columnList = "createdAt"),
    @Index(name = "idx_story_expires", columnList = "expiresAt")
})
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private StoryType type;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "media_url", length = 500)
    private String mediaUrl;
    
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "privacy")
    private StoryPrivacy privacy;
    
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StoryView> views;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum StoryType { TEXT, IMAGE, VIDEO }
    public enum StoryPrivacy { PUBLIC, CONTACTS, CLOSE_FRIENDS, CUSTOM }
}
```

### StoryView Entity
```java
@Entity
@Table(name = "story_views", indexes = {
    @Index(name = "idx_story_view_story", columnList = "storyId"),
    @Index(name = "idx_story_view_user", columnList = "userId")
})
public class StoryView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreationTimestamp
    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;
}
```

## Notification Management

### Notification Entity
```java
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "userId"),
    @Index(name = "idx_notification_type", columnList = "type"),
    @Index(name = "idx_notification_read", columnList = "isRead"),
    @Index(name = "idx_notification_created", columnList = "createdAt")
})
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private NotificationType type;
    
    @Column(name = "title", length = 200)
    private String title;
    
    @Column(name = "message", length = 500)
    private String message;
    
    @Column(name = "data", columnDefinition = "JSON")
    private String data;
    
    @Column(name = "is_read")
    private Boolean isRead;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum NotificationType { MESSAGE, CALL, STORY, CONTACT_REQUEST, GROUP_INVITE }
}
```

## Authentication

### OtpVerification Entity
```java
@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_otp_session", columnList = "tempSessionId"),
    @Index(name = "idx_otp_contact", columnList = "contactInfo"),
    @Index(name = "idx_otp_status", columnList = "status")
})
public class OtpVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "temp_session_id", nullable = false, length = 100)
    private String tempSessionId;
    
    @Column(name = "contact_info", nullable = false, length = 100)
    private String contactInfo;
    
    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private OtpType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OtpStatus status;
    
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "attempts_count")
    private Integer attemptsCount;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum OtpType { EMAIL_LOGIN, PHONE_LOGIN, USERNAME_LOGIN, REGISTRATION, PASSWORD_RESET }
    public enum OtpStatus { PENDING, VERIFIED, EXPIRED, FAILED }
}
```vate Message replyToMessage;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarded_from_message_id")
    private Message forwardedFromMessage;
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageStatus> messageStatuses;
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MessageReaction> reactions;
    
    @Enumerated(EnumType.STRING)
    private MessageStatus status;
    
    private Boolean isEdited;
    private Boolean isDeleted;
    private Boolean isStarred;
    private LocalDateTime timestamp;
    private LocalDateTime editedAt;
    private LocalDateTime deletedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum MessageType { TEXT, IMAGE, VIDEO, AUDIO, DOCUMENT, LOCATION, CONTACT, STICKER }
}
```

### MessageAttachment Entity
```java
@Entity
@Table(name = "message_attachments", indexes = {
    @Index(name = "idx_attachment_message", columnList = "messageId"),
    @Index(name = "idx_attachment_type", columnList = "type")
})
public class MessageAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
    
    @Enumerated(EnumType.STRING)
    private AttachmentType type;
    
    @Column(length = 500)
    private String fileName;
    
    @Column(length = 500)
    private String fileUrl;
    
    @Column(length = 500)
    private String thumbnailUrl;
    
    private Long fileSize;
    
    @Column(length = 100)
    private String mimeType;
    
    private Integer width;
    private Integer height;
    private Integer duration;
    
    @Column(length = 2000)
    private String metadata;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum AttachmentType { IMAGE, VIDEO, AUDIO, DOCUMENT, STICKER }
}
```

### MessageStatus Entity (Denormalized)
```java
@Entity
@Table(name = "message_statuses", indexes = {
    @Index(name = "idx_status_message", columnList = "messageId"),
    @Index(name = "idx_status_user", columnList = "userId"),
    @Index(name = "idx_status_conversation", columnList = "conversationId"),
    @Index(name = "idx_status_type", columnList = "status")
})
public class MessageStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "conversation_id") // Denormalized for performance
    private Long conversationId;
    
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;
    
    private LocalDateTime timestamp;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum DeliveryStatus { SENT, DELIVERED, READ }
}
```

### MessageReaction Entity
```java
@Entity
@Table(name = "message_reactions", indexes = {
    @Index(name = "idx_reaction_message", columnList = "messageId"),
    @Index(name = "idx_reaction_user", columnList = "userId")
})
public class MessageReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(length = 10)
    private String emoji;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

## Call Management

### Call Entity
```java
@Entity
@Table(name = "calls", indexes = {
    @Index(name = "idx_call_conversation", columnList = "conversationId"),
    @Index(name = "idx_call_initiator", columnList = "initiatorId"),
    @Index(name = "idx_call_status", columnList = "status"),
    @Index(name = "idx_call_started", columnList = "startedAt")
})
public class Call {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;
    
    @Enumerated(EnumType.STRING)
    private CallType type;
    
    @Enumerated(EnumType.STRING)
    private CallStatus status;
    
    @OneToMany(mappedBy = "call", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CallParticipant> participants;
    
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer duration;
    
    @Column(length = 2000)
    private String metadata;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum CallType { VOICE, VIDEO }
    public enum CallStatus { INITIATED, RINGING, ANSWERED, ENDED, MISSED, DECLINED }
}
```

### CallParticipant Entity
```java
@Entity
@Table(name = "call_participants", indexes = {
    @Index(name = "idx_call_participant_call", columnList = "callId"),
    @Index(name = "idx_call_participant_user", columnList = "userId")
})
public class CallParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private Call call;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    private CallParticipantStatus status;
    
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Integer duration;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum CallParticipantStatus { INVITED, JOINED, LEFT, DECLINED }
}
```

## Story Management

### Story Entity
```java
@Entity
@Table(name = "stories", indexes = {
    @Index(name = "idx_story_user", columnList = "userId"),
    @Index(name = "idx_story_created", columnList = "createdAt"),
    @Index(name = "idx_story_expires", columnList = "expiresAt")
})
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    private StoryType type;
    
    @Column(length = 1000)
    private String content;
    
    @Column(length = 500)
    private String mediaUrl;
    
    @Column(length = 500)
    private String thumbnailUrl;
    
    @Enumerated(EnumType.STRING)
    private StoryPrivacy privacy;
    
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StoryView> views;
    
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum StoryType { TEXT, IMAGE, VIDEO }
    public enum StoryPrivacy { PUBLIC, CONTACTS, CLOSE_FRIENDS, CUSTOM }
}
```

### StoryView Entity
```java
@Entity
@Table(name = "story_views", indexes = {
    @Index(name = "idx_story_view_story", columnList = "storyId"),
    @Index(name = "idx_story_view_user", columnList = "userId")
})
public class StoryView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreationTimestamp
    private LocalDateTime viewedAt;
}
```

## Notification Management

### Notification Entity
```java
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "userId"),
    @Index(name = "idx_notification_type", columnList = "type"),
    @Index(name = "idx_notification_read", columnList = "isRead"),
    @Index(name = "idx_notification_created", columnList = "createdAt")
})
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    @Column(length = 200)
    private String title;
    
    @Column(length = 500)
    private String message;
    
    @Column(length = 2000)
    private String data;
    
    private Boolean isRead;
    private LocalDateTime readAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum NotificationType { MESSAGE, CALL, STORY, CONTACT_REQUEST, GROUP_INVITE }
}
```

## Authentication

### OtpVerification Entity
```java
@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_otp_session", columnList = "tempSessionId"),
    @Index(name = "idx_otp_contact", columnList = "contactInfo"),
    @Index(name = "idx_otp_status", columnList = "status")
})
public class OtpVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String tempSessionId;
    
    @Column(nullable = false, length = 100)
    private String contactInfo;
    
    @Column(nullable = false, length = 10)
    private String otpCode;
    
    @Enumerated(EnumType.STRING)
    private OtpType type;
    
    @Enumerated(EnumType.STRING)
    private OtpStatus status;
    
    @Column(length = 255)
    private String deviceFingerprint;
    
    @Column(length = 45)
    private String ipAddress;
    
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private Integer attemptsCount;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum OtpType { EMAIL_LOGIN, PHONE_LOGIN, USERNAME_LOGIN, REGISTRATION, PASSWORD_RESET }
    public enum OtpStatus { PENDING, VERIFIED, EXPIRED, FAILED }
}
```
    private Integer duration;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum CallParticipantStatus { INVITED, JOINED, LEFT, DECLINED }
}
```

## Story Management

### Story Entity
```java
@Entity
@Table(name = "stories", indexes = {
    @Index(name = "idx_story_user", columnList = "userId"),
    @Index(name = "idx_story_created", columnList = "createdAt"),
    @Index(name = "idx_story_expires", columnList = "expiresAt")
})
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private StoryType type;
    
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "media_url", length = 500)
    private String mediaUrl;
    
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "privacy")
    private StoryPrivacy privacy;
    
    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StoryView> views;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum StoryType { TEXT, IMAGE, VIDEO }
    public enum StoryPrivacy { PUBLIC, CONTACTS, CLOSE_FRIENDS, CUSTOM }
}
```

### StoryView Entity
```java
@Entity
@Table(name = "story_views", indexes = {
    @Index(name = "idx_story_view_story", columnList = "storyId"),
    @Index(name = "idx_story_view_user", columnList = "userId")
})
public class StoryView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @CreationTimestamp
    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;
}
```

## Notification Management

### Notification Entity
```java
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "userId"),
    @Index(name = "idx_notification_type", columnList = "type"),
    @Index(name = "idx_notification_read", columnList = "isRead"),
    @Index(name = "idx_notification_created", columnList = "createdAt")
})
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private NotificationType type;
    
    @Column(name = "title", length = 200)
    private String title;
    
    @Column(name = "message", length = 500)
    private String message;
    
    @Column(name = "data", columnDefinition = "JSON")
    private String data;
    
    @Column(name = "is_read")
    private Boolean isRead;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum NotificationType { MESSAGE, CALL, STORY, CONTACT_REQUEST, GROUP_INVITE }
}
```

## Error Tracking & Debugging System

### SystemError Entity
```java
@Entity
@Table(name = "system_errors", indexes = {
    @Index(name = "idx_error_correlation", columnList = "correlationId"),
    @Index(name = "idx_error_user", columnList = "userId"),
    @Index(name = "idx_error_severity", columnList = "severity"),
    @Index(name = "idx_error_category", columnList = "errorCategory"),
    @Index(name = "idx_error_created", columnList = "createdAt"),
    @Index(name = "idx_error_resolved", columnList = "isResolved")
})
public class SystemError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "correlation", nullable = false, length = 100)
    private String correlationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private ErrorSeverity severity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "error_category", nullable = false)
    private ErrorCategory errorCategory;
    
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;
    
    @Column(name = "request_url", length = 500)
    private String requestUrl;
    
    @Column(name = "http_method", length = 10)
    private String httpMethod;
    
    @Column(name = "request_headers", columnDefinition = "JSON")
    private String requestHeaders;
    
    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;
    
    @Column(name = "response_status")
    private Integer responseStatus;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 1000)
    private String userAgent;
    
    @Column(name = "device_info", columnDefinition = "JSON")
    private String deviceInfo;
    
    @Column(name = "environment", length = 50)
    private String environment;
    
    @Column(name = "service_name", length = 100)
    private String serviceName;
    
    @Column(name = "method_name", length = 200)
    private String methodName;
    
    @Column(name = "line_number")
    private Integer lineNumber;
    
    @Column(name = "additional_context", columnDefinition = "JSON")
    private String additionalContext;
    
    @Column(name = "is_resolved")
    private Boolean isResolved;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by")
    private String resolvedBy;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum ErrorSeverity { LOW, MEDIUM, HIGH, CRITICAL }
    
    public enum ErrorCategory {
        AUTHENTICATION,
        AUTHORIZATION,
        VALIDATION,
        DATABASE,
        NETWORK,
        FILE_UPLOAD,
        WEBSOCKET,
        EXTERNAL_API,
        BUSINESS_LOGIC,
        SYSTEM,
        UNKNOWN
    }
}
```


## Authentication

### OtpVerification Entity
```java
@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_otp_session", columnList = "tempSessionId"),
    @Index(name = "idx_otp_contact", columnList = "contactInfo"),
    @Index(name = "idx_otp_status", columnList = "status")
})
public class OtpVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "temp_session_id", nullable = false, length = 100)
    private String tempSessionId;
    
    @Column(name = "contact_info", nullable = false, length = 100)
    private String contactInfo;
    
    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private OtpType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OtpStatus status;
    
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "attempts_count")
    private Integer attemptsCount;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum OtpType { EMAIL_LOGIN, PHONE_LOGIN, USERNAME_LOGIN, REGISTRATION, PASSWORD_RESET }
    public enum OtpStatus { PENDING, VERIFIED, EXPIRED, FAILED }
}
```