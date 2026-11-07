package com.sns.marigold.storage.cleanup;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.sns.marigold.storage.entity.StorageFile;
import com.sns.marigold.storage.entity.StorageFileStatus;
import com.sns.marigold.storage.objectstore.ObjectStorage;
import com.sns.marigold.storage.repository.StorageFileRepository;
import com.sns.marigold.storage.service.StorageFileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@ConditionalOnProperty(name = "app.storage.cleanup.mode")
@RequiredArgsConstructor
@Slf4j
public class StorageOrphanCleanupService {

  private static final int LOG_SAMPLE_SIZE = 20;

  private final ObjectStorage objectStorage;
  private final StorageFileService storageFileService;
  private final StorageFileRepository storageFileRepository;
  private final StorageFileInventory inventory;
  private final StorageFileReferenceRepository referenceRepository;
  private final StorageCleanupProperties properties;
  private final AtomicBoolean running = new AtomicBoolean();

  /**
   * 1. 만료된 PENDING 행의 파일을 지우고 DELETED로 바꾼다. 등록되지 않은 업로드의 주 정리 경로다.
   *
   * <p>2. 저장소 전체를 훑어 PENDING·ATTACHED 행이 없는 파일을 지운다. 커밋 후 삭제에 실패한 DELETED 파일 등을 잡는 안전망이다.
   */
  public void cleanup() {
    if (!running.compareAndSet(false, true)) {
      log.info("event=storage_orphan_cleanup_skipped reason=already_running");
      return;
    }

    long startedAt = System.nanoTime();
    CleanupStats stats = new CleanupStats();
    try {
      expirePendingUploads(LocalDateTime.now().minus(properties.minimumAge()), stats);

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
          "event=storage_orphan_cleanup_completed expiredPending={} scanned={} referenced={} candidates={} dryRun={} durationMs={}",
          stats.expiredPending,
          stats.scanned,
          stats.referenced,
          stats.candidates,
          properties.dryRun(),
          Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
    } catch (RuntimeException e) {
      // 앞 페이지의 고아는 이미 지웠을 수 있어서 진행한 만큼 남긴다
      log.error(
          "event=storage_orphan_cleanup_failed expiredPending={} scanned={} candidates={} dryRun={}",
          stats.expiredPending,
          stats.scanned,
          stats.candidates,
          properties.dryRun(),
          e);
      throw e;
    } finally {
      running.set(false);
    }
  }

  // 토큰 TTL이 minimumAge보다 짧아서, 만료된 PENDING은 더 확정될 수 없다
  private void expirePendingUploads(LocalDateTime createdBefore, CleanupStats stats) {
    long afterId = 0;
    List<StorageFile> page;
    do {
      page =
          storageFileRepository.findTop1000ByStatusAndCreatedAtBeforeAndIdGreaterThanOrderByIdAsc(
              StorageFileStatus.PENDING, createdBefore, afterId);
      if (page.isEmpty()) {
        return;
      }
      afterId = page.get(page.size() - 1).getId();
      List<String> fileNames = page.stream().map(StorageFile::getStoredFileName).toList();
      stats.expiredPending += fileNames.size();

      if (!properties.dryRun()) {
        // 파일을 먼저 지운다. 상태 변경이 실패해도 다음 실행이 다시 지우고 바꾼다
        fileNames.forEach(objectStorage::discardUploadBestEffort);
        storageFileService.expirePending(fileNames);
      }
    } while (page.size() == StorageFileInventory.PAGE_SIZE);
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
      objectStorage.deleteFilesBestEffort(orphanFileNames);
    }
  }

  private static final class CleanupStats {
    private long expiredPending;
    private long scanned;
    private long referenced;
    private long candidates;
    private final List<String> candidateSample = new ArrayList<>();
  }
}
