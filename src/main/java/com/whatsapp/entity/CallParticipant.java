package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "call_participants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public enum CallParticipantStatus {
        INVITED, JOINED, LEFT, DECLINED
    }
}