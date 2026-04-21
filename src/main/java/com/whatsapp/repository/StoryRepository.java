package com.whatsapp.repository;

import com.whatsapp.entity.Story;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

  @Query("SELECT s FROM Story s JOIN FETCH s.user u "
      + "LEFT JOIN Contact c ON c.contactUser.id = u.id AND c.user.id = :userId "
      + "WHERE s.expiresAt > CURRENT_TIMESTAMP " + "AND (s.privacy = 'PUBLIC' OR "
      + "     (s.privacy = 'CONTACTS' AND c.id IS NOT NULL) OR " + "     s.user.id = :userId) "
      + "ORDER BY s.createdAt DESC")
  List<Story> findStoryFeed(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT s FROM Story s " + "WHERE s.user.id = :userId " + "ORDER BY s.createdAt DESC")
  List<Story> findUserStories(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT s FROM Story s " + "WHERE s.id = :storyId AND s.user.id = :userId")
  Optional<Story> findByIdAndUserId(@Param("storyId") Long storyId, @Param("userId") Long userId);

  @Query("SELECT s FROM Story s JOIN FETCH s.views sv "
      + "WHERE s.id = :storyId AND s.user.id = :userId")
  Optional<Story> findWithViews(@Param("storyId") Long storyId, @Param("userId") Long userId);

  @Modifying
  @Query("DELETE FROM Story s WHERE s.expiresAt < CURRENT_TIMESTAMP")
  int deleteExpiredStories();

  @Query("SELECT COUNT(s) FROM Story s " + "WHERE s.user.id = :userId AND s.createdAt >= :since")
  long countUserStoriesSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
