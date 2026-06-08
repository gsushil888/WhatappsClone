package com.whatsapp.repository;

import com.whatsapp.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

  @Query("SELECT DISTINCT c FROM Conversation c " + "JOIN c.participants p "
      + "WHERE p.user.id = :userId "
      + "AND (p.status = 'ACTIVE' "
      + "  OR (p.status = 'LEFT' AND EXISTS (SELECT m FROM Message m WHERE m.conversation.id = c.id AND m.isDeleted = false AND m.createdAt > p.clearedAt)) "
      + "  OR (p.status = 'REMOVED' AND c.type = 'GROUP')) "
      + "AND (p.isArchived = false OR p.isArchived IS NULL) "
      + "AND (c.type != 'INDIVIDUAL' OR c.createdBy.id = :userId "
      + "  OR EXISTS (SELECT m FROM Message m WHERE m.conversation.id = c.id AND m.isDeleted = false AND (p.clearedAt IS NULL OR m.createdAt > p.clearedAt))) "
      + "ORDER BY c.updatedAt DESC, c.createdAt DESC")
  List<Conversation> findUserConversations(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT DISTINCT c FROM Conversation c " + "JOIN c.participants p "
      + "WHERE p.user.id = :userId AND p.status = 'ACTIVE' AND (p.isFavorite = true) AND (p.isArchived = false OR p.isArchived IS NULL) "
      + "ORDER BY c.updatedAt DESC, c.createdAt DESC")
  List<Conversation> findFavoriteConversations(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT DISTINCT c FROM Conversation c " + "JOIN c.participants p "
      + "WHERE p.user.id = :userId AND p.status = 'ACTIVE' AND p.isArchived = true "
      + "ORDER BY c.updatedAt DESC, c.createdAt DESC")
  List<Conversation> findArchivedConversations(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT DISTINCT c FROM Conversation c " + "JOIN c.participants p "
      + "WHERE p.user.id = :userId AND p.status = 'ACTIVE' AND (p.isArchived = false OR p.isArchived IS NULL) "
      + "AND EXISTS (SELECT 1 FROM MessageStatus ms WHERE ms.message.conversation.id = c.id AND ms.user.id = :userId AND ms.status IN ('SENT', 'DELIVERED')) "
      + "ORDER BY c.updatedAt DESC, c.createdAt DESC")
  List<Conversation> findUnreadConversations(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT DISTINCT c FROM Conversation c " + "JOIN c.participants p "
      + "JOIN c.participants p2 ON p2.conversation.id = c.id AND p2.user.id != :userId "
      + "JOIN Contact ct ON (ct.user.id = :userId AND ct.contactUser.id = p2.user.id) "
      + "WHERE p.user.id = :userId AND p.status = 'ACTIVE' AND (p.isArchived = false OR p.isArchived IS NULL) AND c.type = 'INDIVIDUAL' AND ct.isBlocked = true "
      + "ORDER BY c.updatedAt DESC, c.createdAt DESC")
  List<Conversation> findBlockedConversations(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT c FROM Conversation c " + "JOIN c.participants p "
      + "WHERE c.id = :conversationId AND p.user.id = :userId "
      + "AND p.status IN ('ACTIVE', 'LEFT', 'REMOVED')")
  Optional<Conversation> findByIdAndUserId(@Param("conversationId") Long conversationId,
      @Param("userId") Long userId);

  // Finds any existing INDIVIDUAL conversation between two users regardless of status
  // (ACTIVE or LEFT) — supports WhatsApp-style re-surface when one user deleted the chat
  @Query("SELECT c FROM Conversation c JOIN c.participants p1 JOIN c.participants p2 "
      + "WHERE c.type = 'INDIVIDUAL' "
      + "AND p1.user.id = :userId1 AND p1.status IN ('ACTIVE', 'LEFT') "
      + "AND p2.user.id = :userId2 AND p2.status IN ('ACTIVE', 'LEFT')")
  Optional<Conversation> findIndividualConversation(@Param("userId1") Long userId1,
      @Param("userId2") Long userId2);

  @Query("SELECT COUNT(DISTINCT c) FROM Conversation c " + "JOIN c.participants p "
      + "WHERE p.user.id = :userId AND p.status = 'ACTIVE'" +"AND p.isArchived = false")
  long countUserConversations(@Param("userId") Long userId);
}
