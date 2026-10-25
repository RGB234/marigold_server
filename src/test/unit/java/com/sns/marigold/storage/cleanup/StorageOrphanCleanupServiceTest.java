package com.sns.marigold.storage.cleanup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sns.marigold.storage.service.StorageService;

@ExtendWith(MockitoExtension.class)
class StorageOrphanCleanupServiceTest {

  private static final String REFERENCED_FILE =
      "user/profile/11111111-1111-1111-1111-111111111111.jpg";
  private static final String OLD_ORPHAN = "adoption/post/22222222-2222-2222-2222-222222222222.jpg";
  private static final String YOUNG_ORPHAN =
      "chat/attachment/33333333-3333-3333-3333-333333333333.txt";
  private static final String OTHER_OLD_ORPHAN =
      "adoption/comment/44444444-4444-4444-4444-444444444444.jpg";

  @Mock private StorageService storageService;
  @Mock private StorageFileInventory inventory;
  @Mock private StorageFileReferenceRepository referenceRepository;

  private StorageOrphanCleanupService cleanupService;

  @BeforeEach
  void setUp() {
    cleanupService =
        new StorageOrphanCleanupService(
            storageService, inventory, referenceRepository, properties(false));
  }

  @Test
  void deletesOnlyOldUnreferencedFiles() {
    Instant now = Instant.now();
    givenPages(
        List.of(
            new StoredFile(REFERENCED_FILE, now.minus(Duration.ofDays(2))),
            new StoredFile(OLD_ORPHAN, now.minus(Duration.ofDays(2))),
            new StoredFile(YOUNG_ORPHAN, now.minus(Duration.ofHours(1)))));
    given(referenceRepository.findReferenced(List.of(REFERENCED_FILE, OLD_ORPHAN)))
        .willReturn(Set.of(REFERENCED_FILE));

    cleanupService.cleanup();

    verify(storageService).deleteFilesBestEffort(List.of(OLD_ORPHAN));
  }

  @Test
  void looksUpAndDeletesPageByPage() {
    Instant old = Instant.now().minus(Duration.ofDays(2));
    givenPages(
        List.of(new StoredFile(OLD_ORPHAN, old)),
        List.of(new StoredFile(REFERENCED_FILE, old), new StoredFile(OTHER_OLD_ORPHAN, old)));
    given(referenceRepository.findReferenced(List.of(OLD_ORPHAN))).willReturn(Set.of());
    given(referenceRepository.findReferenced(List.of(REFERENCED_FILE, OTHER_OLD_ORPHAN)))
        .willReturn(Set.of(REFERENCED_FILE));

    cleanupService.cleanup();

    verify(storageService).deleteFilesBestEffort(List.of(OLD_ORPHAN));
    verify(storageService).deleteFilesBestEffort(List.of(OTHER_OLD_ORPHAN));
  }

  @Test
  void dryRunDoesNotDeleteOrphans() {
    cleanupService =
        new StorageOrphanCleanupService(
            storageService, inventory, referenceRepository, properties(true));
    givenPages(List.of(new StoredFile(OLD_ORPHAN, Instant.now().minus(Duration.ofDays(2)))));
    given(referenceRepository.findReferenced(List.of(OLD_ORPHAN))).willReturn(Set.of());

    cleanupService.cleanup();

    verify(storageService, never()).deleteFilesBestEffort(any());
  }

  @SafeVarargs
  private void givenPages(List<StoredFile>... pages) {
    willAnswer(
            invocation -> {
              Consumer<List<StoredFile>> pageConsumer = invocation.getArgument(0);
              for (List<StoredFile> page : pages) {
                pageConsumer.accept(page);
              }
              return null;
            })
        .given(inventory)
        .forEachPage(any());
  }

  private StorageCleanupProperties properties(boolean dryRun) {
    return new StorageCleanupProperties(
        StorageCleanupMode.OFF, "0 0 4 * * *", "Asia/Seoul", Duration.ofHours(24), dryRun);
  }
}
