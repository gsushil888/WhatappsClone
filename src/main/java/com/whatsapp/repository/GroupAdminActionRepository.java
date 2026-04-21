package com.whatsapp.repository;

import com.whatsapp.entity.GroupAdminAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupAdminActionRepository extends JpaRepository<GroupAdminAction, Long> {

  @Query("SELECT gaa FROM GroupAdminAction gaa " + "JOIN FETCH gaa.adminUser "
      + "LEFT JOIN FETCH gaa.targetUser " + "WHERE gaa.conversation.id = :conversationId "
      + "ORDER BY gaa.createdAt DESC")
  List<GroupAdminAction> findConversationActions(@Param("conversationId") Long conversationId,
      Pageable pageable);

  @Query("SELECT gaa FROM GroupAdminAction gaa " + "WHERE gaa.conversation.id = :conversationId "
      + "AND gaa.adminUser.id = :adminUserId " + "ORDER BY gaa.createdAt DESC")
  List<GroupAdminAction> findActionsByAdmin(@Param("conversationId") Long conversationId,
      @Param("adminUserId") Long adminUserId, Pageable pageable);

  @Query("SELECT gaa FROM GroupAdminAction gaa " + "WHERE gaa.conversation.id = :conversationId "
      + "AND gaa.targetUser.id = :targetUserId " + "ORDER BY gaa.createdAt DESC")
  List<GroupAdminAction> findActionsOnUser(@Param("conversationId") Long conversationId,
      @Param("targetUserId") Long targetUserId, Pageable pageable);

  @Query("SELECT COUNT(gaa) FROM GroupAdminAction gaa "
      + "WHERE gaa.conversation.id = :conversationId " + "AND gaa.createdAt >= :since")
  long countRecentActions(@Param("conversationId") Long conversationId,
      @Param("since") LocalDateTime since);
}
