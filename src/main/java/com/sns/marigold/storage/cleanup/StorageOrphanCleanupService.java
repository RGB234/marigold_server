package com.sns.marigold.storage.cleanup;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
    CleanupStats stats = new CleanupStats();
    try {
      Instant cutoff = Instant.now().minus(properties.minimumAge());
      inventory.forEachPage(page -> cleanupPage(page, cutoff, stats));

      if (stats.candidates > 0) {
        log.warn(
            "event=storage_orphan_cleanup_candidates count={} dryRun={} sample={} sampleTruncated={}",
            stats.candidates,
            properties.dryRun(),
            stats.candidateSample,
            stats.candidates > LOG_SAMPLE_SIZE);
      }

      log.info(
          "event=storage_orphan_cleanup_completed scanned={} referenced={} candidates={} dryRun={} durationMs={}",
          stats.scanned,
          stats.referenced,
          stats.candidates,
          properties.dryRun(),
          Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
    } catch (RuntimeException e) {
      // 앞 페이지의 고아는 이미 지웠을 수 있어서 진행한 만큼 남긴다
      log.error(
          "event=storage_orphan_cleanup_failed scanned={} candidates={} dryRun={}",
          stats.scanned,
          stats.candidates,
          properties.dryRun(),
          e);
      throw e;
    } finally {
      running.set(false);
    }
  }

  // 페이지마다 조회하고 지워서, 메모리에는 한 페이지와 로그 샘플만 남는다
  private void cleanupPage(List<StoredFile> page, Instant cutoff, CleanupStats stats) {
    List<String> agedFileNames =
        page.stream()
            .filter(file -> file.lastModified().isBefore(cutoff))
            .map(StoredFile::storedFileName)
            .toList();
    Set<String> referencedFileNames = referenceRepository.findReferenced(agedFileNames);
    List<String> orphanFileNames =
        agedFileNames.stream().filter(fileName -> !referencedFileNames.contains(fileName)).toList();

    stats.scanned += page.size();
    stats.referenced += referencedFileNames.size();
    stats.candidates += orphanFileNames.size();
    orphanFileNames.stream()
        .limit(LOG_SAMPLE_SIZE - stats.candidateSample.size())
        .forEach(stats.candidateSample::add);

    if (!properties.dryRun() && !orphanFileNames.isEmpty()) {
      storageService.deleteFilesBestEffort(orphanFileNames);
    }
  }

  private static final class CleanupStats {
    private long scanned;
    private long referenced;
    private long candidates;
    private final List<String> candidateSample = new ArrayList<>();
  }
}
