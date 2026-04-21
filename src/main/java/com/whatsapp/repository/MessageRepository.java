package com.whatsapp.repository;

import com.whatsapp.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

  @Query("SELECT m FROM Message m " + "LEFT JOIN FETCH m.attachments "
      + "LEFT JOIN FETCH m.reactions " + "WHERE m.conversation.id = :conversationId "
      + "AND m.isDeleted = false " + "AND (:before IS NULL OR m.createdAt < :before) "
      + "ORDER BY m.createdAt DESC")
  List<Message> findConversationMessages(@Param("conversationId") Long conversationId,
      @Param("before") LocalDateTime before, Pageable pageable);

  @Query("SELECT m FROM Message m " + "JOIN m.conversation c " + "JOIN c.participants p "
      + "WHERE p.user.id = :userId " + "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) "
      + "AND m.isDeleted = false " + "ORDER BY m.createdAt DESC")
  List<Message> searchMessages(@Param("userId") Long userId, @Param("query") String query,
      Pageable pageable);

  @Query("SELECT m FROM Message m " + "JOIN m.messageStatuses ms " + "WHERE ms.user.id = :userId "
      + "AND m.isStarred = true " + "AND m.isDeleted = false " + "ORDER BY m.createdAt DESC")
  List<Message> findStarredMessages(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT COUNT(m) FROM Message m " + "WHERE m.conversation.id = :conversationId "
      + "AND m.isDeleted = false")
  long countConversationMessages(@Param("conversationId") Long conversationId);

  @Query("SELECT m FROM Message m " + "WHERE m.conversation.id = :conversationId "
      + "AND m.sender.id = :userId " + "AND m.id = :messageId")
  Message findByIdAndConversationIdAndSenderId(@Param("messageId") Long messageId,
      @Param("conversationId") Long conversationId, @Param("userId") Long userId);

  @Query("SELECT m FROM Message m " + "WHERE m.conversation.id = :conversationId "
      + "AND m.isDeleted = false " + "ORDER BY m.createdAt DESC")
  List<Message> findByConversationIdOrderByTimestampDesc(
      @Param("conversationId") Long conversationId, Pageable pageable);

  @Query("SELECT COUNT(m) FROM Message m " + "WHERE m.conversation.id = :conversationId "
      + "AND m.isDeleted = false")
  long countByConversationId(@Param("conversationId") Long conversationId);

  @Query("SELECT m FROM Message m " + "WHERE m.conversation.id = :conversationId "
      + "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) " + "AND m.isDeleted = false "
      + "ORDER BY m.createdAt DESC")
  List<Message> searchInConversation(@Param("conversationId") Long conversationId,
      @Param("query") String query, Pageable pageable);

  @Query("SELECT m FROM Message m " + "LEFT JOIN FETCH m.sender "
      + "WHERE m.conversation.id = :conversationId " + "AND m.isDeleted = false "
      + "ORDER BY m.createdAt DESC")
  List<Message> findLastMessageByConversationId(@Param("conversationId") Long conversationId,
      Pageable pageable);

  @Query("SELECT m FROM Message m " + "LEFT JOIN FETCH m.sender " + "LEFT JOIN FETCH m.attachments "
      + "WHERE m.conversation.id = :conversationId " + "AND m.isDeleted = false "
      + "AND m.type IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT') " + "ORDER BY m.createdAt DESC")
  List<Message> findMediaMessages(@Param("conversationId") Long conversationId, Pageable pageable);

  @Query("SELECT m FROM Message m " + "WHERE m.conversation.id = :conversationId "
      + "AND m.id < :messageId " + "AND m.isDeleted = false " + "ORDER BY m.id DESC")
  List<Message> findMessagesBeforeId(@Param("conversationId") Long conversationId,
      @Param("messageId") Long messageId, Pageable pageable);

  @Query("SELECT m FROM Message m " + "WHERE m.conversation.id = :conversationId "
      + "AND m.id > :messageId " + "AND m.isDeleted = false " + "ORDER BY m.id ASC")
  List<Message> findMessagesAfterId(@Param("conversationId") Long conversationId,
      @Param("messageId") Long messageId, Pageable pageable);

  List<Message> findByConversationId(Long conversationId);

  void deleteByConversationId(Long conversationId);
}
