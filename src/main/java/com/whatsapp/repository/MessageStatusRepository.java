package com.whatsapp.repository;

import com.whatsapp.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, Long> {

  @Query("SELECT ms FROM MessageStatus ms JOIN FETCH ms.user " + "WHERE ms.message.id = :messageId "
      + "ORDER BY ms.timestamp DESC")
  List<MessageStatus> findByMessageId(@Param("messageId") Long messageId);

  @Query("SELECT COUNT(ms) FROM MessageStatus ms "
      + "WHERE ms.message.conversation.id = :conversationId " + "AND ms.user.id = :userId "
      + "AND ms.status = 'DELIVERED' " + "AND ms.message.sender.id != :userId")
  long countUnreadMessages(@Param("conversationId") Long conversationId,
      @Param("userId") Long userId);

  @Modifying
  @Query("INSERT INTO MessageStatus (message, user, conversationId, status, timestamp) "
      + "SELECT m, cp.user, m.conversation.id, 'SENT', CURRENT_TIMESTAMP "
      + "FROM Message m JOIN ConversationParticipant cp ON cp.conversation.id = m.conversation.id "
      + "WHERE m.id = :messageId AND cp.user.id != m.sender.id AND cp.status = 'ACTIVE'")
  int createStatusForParticipants(@Param("messageId") Long messageId);

  @Modifying
  @Query("UPDATE MessageStatus ms SET ms.status = :status, ms.timestamp = :timestamp "
      + "WHERE ms.message.id = :messageId AND ms.user.id = :userId")
  int updateMessageStatus(@Param("messageId") Long messageId, @Param("userId") Long userId,
      @Param("status") MessageStatus.DeliveryStatus status,
      @Param("timestamp") LocalDateTime timestamp);

  void deleteByMessageId(Long messageId);
}
