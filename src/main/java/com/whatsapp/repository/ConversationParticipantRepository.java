package com.whatsapp.repository;

import com.whatsapp.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {

    @Query("SELECT cp FROM ConversationParticipant cp JOIN FETCH cp.user " +
           "WHERE cp.conversation.id = :conversationId AND cp.status = 'ACTIVE' " +
           "ORDER BY cp.role DESC, cp.joinedAt ASC")
    List<ConversationParticipant> findActiveParticipants(@Param("conversationId") Long conversationId);

    @Query("SELECT cp FROM ConversationParticipant cp JOIN FETCH cp.user " +
           "WHERE cp.conversation.id = :conversationId AND cp.status = :status " +
           "ORDER BY cp.role DESC, cp.joinedAt ASC")
    List<ConversationParticipant> findByConversationIdAndStatus(@Param("conversationId") Long conversationId,
                                                                @Param("status") ConversationParticipant.ParticipantStatus status);

    @Query("SELECT cp FROM ConversationParticipant cp " +
           "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId AND cp.status = 'ACTIVE'")
    Optional<ConversationParticipant> findByConversationIdAndUserId(@Param("conversationId") Long conversationId, 
                                                                   @Param("userId") Long userId);

    @Query("SELECT COUNT(cp) FROM ConversationParticipant cp " +
           "WHERE cp.conversation.id = :conversationId AND cp.status = 'ACTIVE'")
    long countActiveParticipants(@Param("conversationId") Long conversationId);

    @Query("SELECT cp FROM ConversationParticipant cp " +
           "WHERE cp.conversation.id = :conversationId AND cp.role IN ('ADMIN', 'OWNER') AND cp.status = 'ACTIVE'")
    List<ConversationParticipant> findAdmins(@Param("conversationId") Long conversationId);

    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.lastReadAt = :readAt, cp.lastReadMessageId = :messageId " +
           "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    int updateLastRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId, 
                      @Param("readAt") LocalDateTime readAt, @Param("messageId") Long messageId);

    @Modifying
    @Query("UPDATE ConversationParticipant cp SET cp.status = 'LEFT', cp.leftAt = CURRENT_TIMESTAMP " +
           "WHERE cp.conversation.id = :conversationId AND cp.user.id = :userId")
    int leaveConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}