package com.sns.marigold.user.event;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sns.marigold.global.storage.service.S3Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

  private final S3Service s3Service;

  @Async // 비동기 실행 (설정 필요, 미설정 시 동기로 동작)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleUserDeleted(UserDeletedEvent event) {
    List<String> storeFileNames = event.getStoreFileNames();

    if (storeFileNames == null || storeFileNames.isEmpty()) {
      return;
    }

    log.info("회원 탈퇴로 인한 이미지 삭제 시작. 대상 파일 수: {}", storeFileNames.size());

    try {
      s3Service.deleteUploadedImagesFromS3ByStoreFileNames(storeFileNames);
    } catch (Exception e) {
      log.error("S3 이미지 삭제 중 오류 발생", e);
    }
  }
}
