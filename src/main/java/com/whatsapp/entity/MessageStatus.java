package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_statuses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    private Long conversationId; // Denormalized for performance

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private LocalDateTime timestamp;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum DeliveryStatus {
        SENT, DELIVERED, READ
    }
}