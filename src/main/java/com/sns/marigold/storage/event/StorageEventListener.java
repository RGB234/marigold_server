package com.sns.marigold.storage.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sns.marigold.storage.objectstore.ObjectStorage;
import com.sns.marigold.storage.service.StorageFileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageEventListener {
  private final ObjectStorage objectStorage;
  private final StorageFileService storageFileService;

  // 발행한 트랜잭션 안에서 동기로 실행되어 도메인 행 변경과 함께 커밋·롤백된다
  @EventListener
  public void markOldStorageFilesDeleted(DeleteOldStorageFilesEvent event) {
    if (event.fileNames() == null || event.fileNames().isEmpty()) return;

    storageFileService.markDeleted(event.fileNames());
  }

  // 프로젝트 어디서든 DeleteOldStorageFilesEvent 발생하고 트랜잭션이 커밋되면 실행됨
  @Async("storageTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDeleteOldStorageFilesEvent(DeleteOldStorageFilesEvent event) {
    if (event.fileNames() == null || event.fileNames().isEmpty()) return;

    log.debug("Deleting old files from storage. count={}", event.fileNames().size());
    objectStorage.deleteFilesBestEffort(event.fileNames());
  }
}
