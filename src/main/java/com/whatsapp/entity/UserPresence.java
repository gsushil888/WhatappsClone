package com.whatsapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_presence")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPresence {

  @Id
  private Long userId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Builder.Default
  private Status status = Status.OFFLINE;

  @UpdateTimestamp
  private LocalDateTime lastSeen;

  private LocalDateTime lastOnline;

  private String deviceInfo;

  @Builder.Default
  private boolean showLastSeen = true;

  public enum Status {
    ONLINE, OFFLINE, AWAY, BUSY
  }
}
