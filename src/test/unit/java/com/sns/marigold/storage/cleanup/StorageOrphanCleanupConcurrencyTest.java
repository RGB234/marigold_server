package com.sns.marigold.storage.cleanup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sns.marigold.storage.service.StorageService;

class StorageOrphanCleanupConcurrencyTest {

  @Test
  void skipsAnOverlappingRun() throws Exception {
    StorageService storageService = mock(StorageService.class);
    StorageFileInventory inventory = mock(StorageFileInventory.class);
    StorageFileReferenceRepository referenceRepository = mock(StorageFileReferenceRepository.class);
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    given(inventory.listStoredFiles())
        .willAnswer(
            invocation -> {
              entered.countDown();
              assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
              return List.of();
            });
    given(referenceRepository.findAllStoredFileNames()).willReturn(Set.of());
    StorageOrphanCleanupService cleanupService =
        new StorageOrphanCleanupService(
            storageService,
            inventory,
            referenceRepository,
            new StorageCleanupProperties(
                StorageCleanupMode.BOTH, "0 0 4 * * *", "Asia/Seoul", Duration.ofHours(24), true));
    ExecutorService executor = Executors.newSingleThreadExecutor();

    try {
      Future<?> firstRun = executor.submit(cleanupService::cleanup);
      assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

      cleanupService.cleanup();
      release.countDown();
      firstRun.get(5, TimeUnit.SECONDS);

      verify(inventory, times(1)).listStoredFiles();
      verify(referenceRepository, times(1)).findAllStoredFileNames();
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
  }
}
