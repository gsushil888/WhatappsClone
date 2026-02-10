package com.whatsapp.repository;

import com.whatsapp.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {

    @Query("SELECT mr FROM MessageReaction mr JOIN FETCH mr.user " +
           "WHERE mr.message.id = :messageId " +
           "ORDER BY mr.createdAt ASC")
    List<MessageReaction> findByMessageId(@Param("messageId") Long messageId);

    @Query("SELECT mr FROM MessageReaction mr " +
           "WHERE mr.message.id = :messageId AND mr.user.id = :userId AND mr.emoji = :emoji")
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(@Param("messageId") Long messageId, 
                                                              @Param("userId") Long userId, 
                                                              @Param("emoji") String emoji);

    @Query("SELECT mr.emoji, COUNT(mr) FROM MessageReaction mr " +
           "WHERE mr.message.id = :messageId " +
           "GROUP BY mr.emoji " +
           "ORDER BY COUNT(mr) DESC")
    List<Object[]> getReactionCounts(@Param("messageId") Long messageId);

    void deleteByMessageIdAndUserIdAndEmoji(Long messageId, Long userId, String emoji);
}