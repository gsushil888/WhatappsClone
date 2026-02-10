package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_errors", indexes = {
    @Index(name = "idx_error_correlation", columnList = "correlationId"),
    @Index(name = "idx_error_user", columnList = "userId"),
    @Index(name = "idx_error_severity", columnList = "severity"),
    @Index(name = "idx_error_category", columnList = "errorCategory"),
    @Index(name = "idx_error_created", columnList = "createdAt"),
    @Index(name = "idx_error_resolved", columnList = "isResolved")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "correlation_id", nullable = false, length = 100)
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
    private boolean isResolved;

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