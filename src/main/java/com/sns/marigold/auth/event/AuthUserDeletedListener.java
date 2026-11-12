package com.sns.marigold.auth.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sns.marigold.auth.common.service.AuthSessionService;
import com.sns.marigold.global.error.exception.ApplicationException;
import com.sns.marigold.user.event.UserDeletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthUserDeletedListener {
  private final AuthSessionService authSessionService;

  // Redis는 DB 트랜잭션에 묶이지 않으므로 탈퇴가 커밋된 뒤 모든 세션을 폐기한다.
  // 실패해도 탈퇴는 유지되고, 남은 세션의 refresh는 DB 상태 검사로 막히며 access token은 만료(기본 10분)까지 쓰인다
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserDeletedEvent event) {
    try {
      authSessionService.endAll(event.userId());
    } catch (ApplicationException e) {
      log.warn(
          "Failed to revoke auth sessions of deleted user: userId={} code={}",
          event.userId(),
          e.getErrorSpec().code());
    }
  }
}
