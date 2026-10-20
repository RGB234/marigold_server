package com.sns.marigold.storage.cleanup;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.sns.marigold.storage.service.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "app.storage.cleanup.mode")
@RequiredArgsConstructor
@Slf4j
public class StorageOrphanCleanupService {

  private static final int LOG_SAMPLE_SIZE = 20;

  private final StorageService storageService;
  private final StorageFileInventory inventory;
  private final StorageFileReferenceRepository referenceRepository;
  private final StorageCleanupProperties properties;
  private final AtomicBoolean running = new AtomicBoolean();

  public void cleanup() {
    if (!running.compareAndSet(false, true)) {
      log.info("event=storage_orphan_cleanup_skipped reason=already_running");
      return;
    }

    long startedAt = System.nanoTime();
    try {
      List<StoredFile> storedFiles = inventory.listStoredFiles();
      Set<String> referencedFileNames = referenceRepository.findAllStoredFileNames();
      Instant cutoff = Instant.now().minus(properties.minimumAge());
      List<String> orphanFileNames = findOrphans(storedFiles, referencedFileNames, cutoff);

      if (!orphanFileNames.isEmpty()) {
        log.warn(
            "event=storage_orphan_cleanup_candidates count={} dryRun={} sample={} sampleTruncated={}",
            orphanFileNames.size(),
            properties.dryRun(),
            orphanFileNames.stream().limit(LOG_SAMPLE_SIZE).toList(),
            orphanFileNames.size() > LOG_SAMPLE_SIZE);
      }

      if (!properties.dryRun() && !orphanFileNames.isEmpty()) {
        storageService.deleteFilesBestEffort(orphanFileNames);
      }

      log.info(
          "event=storage_orphan_cleanup_completed scanned={} referenced={} candidates={} dryRun={} durationMs={}",
          storedFiles.size(),
          referencedFileNames.size(),
          orphanFileNames.size(),
          properties.dryRun(),
          Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
    } catch (RuntimeException e) {
      log.error("event=storage_orphan_cleanup_failed", e);
      throw e;
    } finally {
      running.set(false);
    }
  }

  private List<String> findOrphans(
      List<StoredFile> storedFiles, Set<String> referencedFileNames, Instant cutoff) {
    return storedFiles.stream()
        .filter(file -> file.lastModified().isBefore(cutoff))
        .map(StoredFile::storedFileName)
        .filter(fileName -> !referencedFileNames.contains(fileName))
        .toList();
  }
}
