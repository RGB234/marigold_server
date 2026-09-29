package com.sns.marigold.global.error;

/**
 * 전송 프로토콜과 독립적인 애플리케이션 오류 계약입니다.
 *
 * <p>HTTP 상태나 STOMP 프레임 같은 표현 정책은 각 경계 계층에서 결정합니다.
 */
public interface ErrorSpec {

  String code();

  String publicMessage();

  FailureKind kind();
}
