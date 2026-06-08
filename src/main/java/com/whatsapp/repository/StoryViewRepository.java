package com.whatsapp.repository;

import com.whatsapp.entity.StoryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, Long> {

    boolean existsByStoryIdAndUserId(Long storyId, Long userId);

    @Modifying
    @Query("DELETE FROM StoryView sv WHERE sv.story.id IN :storyIds")
    void deleteByStoryIdIn(@Param("storyIds") List<Long> storyIds);

    @Query("SELECT sv FROM StoryView sv JOIN FETCH sv.user WHERE sv.story.id = :storyId ORDER BY sv.viewedAt DESC")
    List<StoryView> findByStoryId(@Param("storyId") Long storyId);
}
