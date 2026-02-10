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

    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.user.id = :userId AND p.status = 'ACTIVE' " +
           "ORDER BY c.lastMessageAt DESC NULLS LAST, c.createdAt DESC")
    List<Conversation> findUserConversations(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE c.id = :conversationId AND p.user.id = :userId AND p.status = 'ACTIVE'")
    Optional<Conversation> findByIdAndUserId(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p1 " +
           "JOIN c.participants p2 " +
           "WHERE c.type = 'INDIVIDUAL' " +
           "AND p1.user.id = :userId1 AND p1.status = 'ACTIVE' " +
           "AND p2.user.id = :userId2 AND p2.status = 'ACTIVE'")
    Optional<Conversation> findIndividualConversation(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT COUNT(DISTINCT c) FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.user.id = :userId AND p.status = 'ACTIVE'")
    long countUserConversations(@Param("userId") Long userId);
}