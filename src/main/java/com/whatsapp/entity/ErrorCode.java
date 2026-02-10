package com.whatsapp.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Authentication Errors (AUTH_xxx)
    AUTH_USER_NOT_FOUND("AUTH_001", "User not found", HttpStatus.NOT_FOUND),
    AUTH_INVALID_CREDENTIALS("AUTH_002", "Password is required for email/username login", HttpStatus.UNAUTHORIZED),
    AUTH_AUTHENTICATION_FAILED("AUTH_003", "Authentication failed", HttpStatus.UNAUTHORIZED),
    AUTH_SESSION_EXPIRED("AUTH_004", "Session expired", HttpStatus.UNAUTHORIZED),
    AUTH_INVALID_TOKEN("AUTH_005", "Invalid token", HttpStatus.UNAUTHORIZED),
    AUTH_ACCOUNT_SUSPENDED("AUTH_006", "Account suspended", HttpStatus.FORBIDDEN),
    AUTH_REGISTRATION_FAILED("AUTH_007", "Registration failed", HttpStatus.BAD_REQUEST),
    AUTH_OTP_INVALID("AUTH_008", "Invalid OTP", HttpStatus.BAD_REQUEST),
    AUTH_OTP_EXPIRED("AUTH_009", "OTP expired", HttpStatus.BAD_REQUEST),
    AUTH_OTP_ATTEMPTS_EXCEEDED("AUTH_010", "OTP attempts exceeded", HttpStatus.TOO_MANY_REQUESTS),
    AUTH_DEVICE_NOT_AUTHORIZED("AUTH_011", "Device not authorized", HttpStatus.FORBIDDEN),
    AUTH_PASSWORD_REQUIRED("AUTH_012", "Password required for username login", HttpStatus.BAD_REQUEST),
    AUTH_SESSION_NOT_FOUND("AUTH_013", "Session not found", HttpStatus.NOT_FOUND),
    
    // User Management Errors (USER_xxx)
    USER_NOT_FOUND("USER_001", "User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("USER_002", "User already exists", HttpStatus.CONFLICT),
    USER_UPDATE_FAILED("USER_003", "User update failed", HttpStatus.BAD_REQUEST),
    USER_INVALID_DATA("USER_004", "Invalid user data", HttpStatus.BAD_REQUEST),
    USER_PROFILE_NOT_FOUND("USER_005", "User profile not found", HttpStatus.NOT_FOUND),
    USER_PERMISSION_DENIED("USER_006", "Permission denied", HttpStatus.FORBIDDEN),
    USER_CONTACT_NOT_FOUND("USER_007", "Contact not found", HttpStatus.NOT_FOUND),
    USER_CONTACT_ALREADY_EXISTS("USER_008", "Contact already exists", HttpStatus.CONFLICT),
    USER_BLOCKED("USER_009", "User is blocked", HttpStatus.FORBIDDEN),
    
    // Contact Errors (CONTACT_xxx)
    CONTACT_ALREADY_EXISTS("CONTACT_001", "Contact already exists", HttpStatus.CONFLICT),
    CONTACT_NOT_FOUND("CONTACT_002", "Contact not found", HttpStatus.NOT_FOUND),
    CONTACT_BLOCKED("CONTACT_003", "Contact is blocked", HttpStatus.FORBIDDEN),
    
    // Conversation Errors (CONV_xxx)
    CONV_NOT_FOUND("CONV_001", "Conversation not found", HttpStatus.NOT_FOUND),
    CONV_ACCESS_DENIED("CONV_002", "Access denied to conversation", HttpStatus.FORBIDDEN),
    CONV_CREATION_FAILED("CONV_003", "Failed to create conversation", HttpStatus.BAD_REQUEST),
    CONV_PARTICIPANT_NOT_FOUND("CONV_004", "Participant not found", HttpStatus.NOT_FOUND),
    CONV_ALREADY_EXISTS("CONV_005", "Conversation already exists", HttpStatus.CONFLICT),
    CONV_INVALID_TYPE("CONV_006", "Invalid conversation type", HttpStatus.BAD_REQUEST),
    CONVERSATION_NOT_FOUND("CONV_007", "Conversation not found", HttpStatus.NOT_FOUND),
    
    // Message Errors (MSG_xxx)
    MSG_NOT_FOUND("MSG_001", "Message not found", HttpStatus.NOT_FOUND),
    MSG_SEND_FAILED("MSG_002", "Failed to send message", HttpStatus.BAD_REQUEST),
    MSG_DELETE_FAILED("MSG_003", "Failed to delete message", HttpStatus.BAD_REQUEST),
    MSG_INVALID_TYPE("MSG_004", "Invalid message type", HttpStatus.BAD_REQUEST),
    MSG_EDIT_FAILED("MSG_005", "Failed to edit message", HttpStatus.BAD_REQUEST),
    MSG_DELIVERY_FAILED("MSG_006", "Message delivery failed", HttpStatus.SERVICE_UNAVAILABLE),
    MSG_TOO_LARGE("MSG_007", "Message too large", HttpStatus.PAYLOAD_TOO_LARGE),
    
    // Call Errors (CALL_xxx)
    CALL_NOT_FOUND("CALL_001", "Call not found", HttpStatus.NOT_FOUND),
    CALL_INITIATION_FAILED("CALL_002", "Failed to initiate call", HttpStatus.BAD_REQUEST),
    CALL_CONNECTION_FAILED("CALL_003", "Call connection failed", HttpStatus.SERVICE_UNAVAILABLE),
    CALL_PARTICIPANT_BUSY("CALL_004", "Participant is busy", HttpStatus.CONFLICT),
    CALL_DECLINED("CALL_005", "Call declined", HttpStatus.BAD_REQUEST),
    CALL_ENDED("CALL_006", "Call ended", HttpStatus.BAD_REQUEST),
    CALL_NOT_ACTIVE("CALL_007", "Call is not active", HttpStatus.BAD_REQUEST),
    
    // Story Errors (STORY_xxx)
    STORY_NOT_FOUND("STORY_001", "Story not found", HttpStatus.NOT_FOUND),
    STORY_CREATION_FAILED("STORY_002", "Failed to create story", HttpStatus.BAD_REQUEST),
    STORY_ACCESS_DENIED("STORY_003", "Access denied to story", HttpStatus.FORBIDDEN),
    STORY_EXPIRED("STORY_004", "Story has expired", HttpStatus.GONE),
    STORY_INVALID_MEDIA("STORY_005", "Invalid story media", HttpStatus.BAD_REQUEST),
    STORY_LIMIT_EXCEEDED("STORY_006", "Story limit exceeded", HttpStatus.BAD_REQUEST),
    
    // Search Errors (SEARCH_xxx)
    SEARCH_QUERY_TOO_SHORT("SEARCH_001", "Search query too short", HttpStatus.BAD_REQUEST),
    SEARCH_INVALID_TYPE("SEARCH_002", "Invalid search type", HttpStatus.BAD_REQUEST),
    SEARCH_LIMIT_EXCEEDED("SEARCH_003", "Search limit exceeded", HttpStatus.BAD_REQUEST),
    
    // File/Media Errors (FILE_xxx)
    FILE_UPLOAD_FAILED("FILE_001", "File upload failed", HttpStatus.BAD_REQUEST),
    FILE_NOT_FOUND("FILE_002", "File not found", HttpStatus.NOT_FOUND),
    FILE_INVALID_TYPE("FILE_003", "Invalid file type", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("FILE_004", "File size exceeded", HttpStatus.PAYLOAD_TOO_LARGE),
    FILE_PROCESSING_FAILED("FILE_005", "File processing failed", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_VIRUS_DETECTED("FILE_006", "Virus detected in file", HttpStatus.BAD_REQUEST),
    
    // WebSocket Errors (WS_xxx)
    WS_CONNECTION_FAILED("WS_001", "WebSocket connection failed", HttpStatus.BAD_REQUEST),
    WS_AUTHENTICATION_FAILED("WS_002", "WebSocket authentication failed", HttpStatus.UNAUTHORIZED),
    WS_MESSAGE_FAILED("WS_003", "Failed to send WebSocket message", HttpStatus.BAD_REQUEST),
    WS_SUBSCRIPTION_FAILED("WS_004", "WebSocket subscription failed", HttpStatus.BAD_REQUEST),
    
    // System Errors (SYS_xxx)
    SYS_INTERNAL_ERROR("SYS_001", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SYS_VALIDATION_ERROR("SYS_002", "Validation error", HttpStatus.BAD_REQUEST),
    SYS_RESOURCE_NOT_FOUND("SYS_003", "Resource not found", HttpStatus.NOT_FOUND),
    SYS_ACCESS_DENIED("SYS_004", "Access denied", HttpStatus.FORBIDDEN),
    SYS_RATE_LIMIT_EXCEEDED("SYS_005", "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    SYS_SERVICE_UNAVAILABLE("SYS_006", "Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    SYS_DATABASE_ERROR("SYS_007", "Database operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    SYS_CACHE_ERROR("SYS_008", "Cache operation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    SYS_EXTERNAL_SERVICE_ERROR("SYS_009", "External service error", HttpStatus.BAD_GATEWAY),
    RESOURCE_NOT_FOUND("SYS_010", "Resource not found", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}