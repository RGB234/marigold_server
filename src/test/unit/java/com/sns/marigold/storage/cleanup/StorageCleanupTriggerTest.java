package com.sns.marigold.storage.cleanup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class StorageCleanupTriggerTest {

  @Test
  void startupModeRunsOnlyOnApplicationReady() {
    StorageOrphanCleanupService cleanupService = mock(StorageOrphanCleanupService.class);
    StorageCleanupTrigger trigger = trigger(StorageCleanupMode.STARTUP, cleanupService);

    trigger.onApplicationReady();
    trigger.runScheduledCleanup();

    verify(cleanupService).cleanup();
  }

  @Test
  void bothModeRunsForBothTriggers() {
    StorageOrphanCleanupService cleanupService = mock(StorageOrphanCleanupService.class);
    StorageCleanupTrigger trigger = trigger(StorageCleanupMode.BOTH, cleanupService);

    trigger.onApplicationReady();
    trigger.runScheduledCleanup();

    verify(cleanupService, times(2)).cleanup();
  }

  @Test
  void offModeNeverRunsCleanup() {
    StorageOrphanCleanupService cleanupService = mock(StorageOrphanCleanupService.class);
    StorageCleanupTrigger trigger = trigger(StorageCleanupMode.OFF, cleanupService);

    trigger.onApplicationReady();
    trigger.runScheduledCleanup();

    verify(cleanupService, never()).cleanup();
  }

  private StorageCleanupTrigger trigger(
      StorageCleanupMode mode, StorageOrphanCleanupService cleanupService) {
    return new StorageCleanupTrigger(
        cleanupService,
        new StorageCleanupProperties(
            mode, "0 0 4 * * *", "Asia/Seoul", Duration.ofHours(24), true));
  }
}
