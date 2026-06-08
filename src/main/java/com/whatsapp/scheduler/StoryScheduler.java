package com.whatsapp.scheduler;

import com.whatsapp.service.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StoryScheduler {

    private final StoryService storyService;

    @Scheduled(fixedRate = 3600000) // every 1 hour
    public void cleanupExpiredStories() {
        try {
            storyService.cleanupExpiredStories();
        } catch (Exception e) {
            log.error("Error during story cleanup: ", e);
        }
    }
}
