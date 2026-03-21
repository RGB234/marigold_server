package com.sns.marigold.storage.event;

import com.sns.marigold.storage.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageEventListener {
  private final S3Service s3Service;

  // 프로젝트 어디서든 StorageFileDeleteEvent가 발생하고 트랜잭션이 커밋되면 실행됨
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDeleteOldStorageFilesEvent(DeleteOldStorageFilesEvent event) {
    if (event.fileNames() == null || event.fileNames().isEmpty()) return;

    log.info("Deleting old files from storage: {}", event.fileNames());
    s3Service.deleteUploadedImagesFromS3ByStoredFileNames(event.fileNames());
  }

  // 트랜잭션 롤백 시 방금 올린 파일들 삭제
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
  public void handleDeleteUploadedStorageFilesEvent(DeleteUploadedStorageFilesEvent event) {
    if (event.fileNames() == null || event.fileNames().isEmpty()) return;

    log.error("Transaction rolled back. Reverting new files from storage: {}", event.fileNames());
    s3Service.deleteUploadedImagesFromS3ByStoredFileNames(event.fileNames());
  }
}
