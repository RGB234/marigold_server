package com.sns.marigold.storage.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sns.marigold.storage.service.S3Service;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StorageFileEventListener {
        
    private final S3Service s3Service;

    // DB 트랜잭션이 무사히 COMMIT 된 직후에만 아래 메서드가 실행됩니다.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeleteEvent(StorageFileDeleteEvent event) {
        s3Service.deleteFile(event.fileName());
    }
}