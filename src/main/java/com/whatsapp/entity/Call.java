package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "calls")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Call {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caller_id", nullable = false)
    private User caller;

    @Enumerated(EnumType.STRING)
    private CallType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CallStatus status = CallStatus.INITIATED;

    private String callToken;
    private LocalDateTime startedAt;
    private LocalDateTime answeredAt;
    private LocalDateTime endedAt;
    private Integer durationSeconds;
    private String endReason;
    private Integer qualityRating;
    private String metadata;

    @OneToMany(mappedBy = "call", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CallParticipant> participants;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum CallType { VOICE, VIDEO }
    public enum CallStatus { INITIATED, RINGING, ANSWERED, ENDED, DECLINED, MISSED, FAILED }
}