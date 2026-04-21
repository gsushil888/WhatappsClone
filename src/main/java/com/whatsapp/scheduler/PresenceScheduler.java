package com.whatsapp.scheduler;

import com.whatsapp.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceScheduler {

  private final PresenceService presenceService;

  @Scheduled(fixedRate = 30000) // Every 30 seconds
  public void cleanupOfflineUsers() {
    try {
      presenceService.cleanupOfflineUsers();
    } catch (Exception e) {
      log.error("Error during presence cleanup: ", e);
    }
  }
}
