package com.sns.marigold.storage.cleanup;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@ConditionalOnProperty(name = "app.storage.cleanup.mode")
@RequiredArgsConstructor
public class StorageCleanupTrigger {

  private final StorageOrphanCleanupService cleanupService;
  private final StorageCleanupProperties properties;

  @Async("storageTaskExecutor")
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    if (properties.mode().runsOnStartup()) {
      cleanupService.cleanup();
    }
  }

  @Scheduled(
      cron = "${app.storage.cleanup.cron:0 0 4 * * *}",
      zone = "${app.storage.cleanup.zone:Asia/Seoul}")
  public void runScheduledCleanup() {
    if (properties.mode().runsOnSchedule()) {
      cleanupService.cleanup();
    }
  }
}
