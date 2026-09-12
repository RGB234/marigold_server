# Architecture

이 문서는 코드 위치와 책임을 빠르게 찾기 위한 안내서입니다.

## 패키지 구조

| 패키지 | 책임 |
| --- | --- |
| `adoption` | 입양 게시글, 댓글, 이미지, 입양 완료 처리 |
| `auth` | 로컬 로그인, OAuth2, JWT, CSRF, security filter chain |
| `chat` | 채팅방, 메시지, 첨부파일, WebSocket/STOMP |
| `storage` | 프로필 기반 파일 업로드, 삭제, 접근 URL 생성 |
| `user` | 사용자 계정, 프로필, 계정 상태 |
| `global` | 공통 응답, 에러 처리, 설정, validation, TSID 변환 |
| `audit` | 보안/인가 관련 감사 로그 |

## 계층 규칙

일반 흐름은 아래 순서입니다.

```text
Controller -> Service -> Repository -> Entity
```

Controller는 HTTP 요청/응답, 인증 principal, validation을 다룹니다. Service는 도메인 규칙과 트랜잭션을 담당합니다. Repository는 Spring Data JPA query를 담당합니다.

## 공통 응답과 예외

API 응답은 `global.dto.ApiResult`로 감쌉니다.

커스텀 예외는 `BusinessException`을 상속하고 `ErrorCode`를 가집니다. `GlobalExceptionHandler`가 비즈니스 예외, validation 예외, 인증/인가 예외, 미처리 예외를 공통 응답으로 변환합니다.

## 인증 구조

Security filter chain은 두 개입니다.

| 우선순위 | 대상 | 설정 |
| --- | --- | --- |
| 1 | `/oauth2/**` | OAuth2 로그인과 callback |
| 2 | OAuth2 외 전체 | JWT, CORS, custom CSRF filter, logout |

필터 체인 레벨에서는 대부분의 요청을 통과시키고, 실제 API 접근 권한은 컨트롤러의 `@PreAuthorize`에서 판단합니다.

JWT 인증은 `Authorization: Bearer ...` 헤더를 읽어 `SecurityContext`에 인증 객체를 저장합니다. 쿠키 기반 인증 상태가 있는 unsafe method는 CSRF double-submit 검증을 통과해야 합니다.

## WebSocket 구조

`chat.config.WebSocketConfig`가 STOMP endpoint와 broker prefix를 설정합니다.

- endpoint: `/ws`
- application destination prefix: `/pub`
- simple broker prefix: `/sub`
- 채팅 메시지 publish: `/pub/chat/message`
- 채팅방 구독: `/sub/chat/room/{roomId}`

WebSocket에서는 `CONNECT` 단계에서 CSRF와 JWT를 검증합니다. 채팅방 구독은 `RoomParticipantRepository`로 참여 여부를 확인합니다.

## 저장소 구조

`storage.service.StorageService`가 파일 업로드, 삭제, 접근 URL 생성을 담당합니다. `local` 프로필은 로컬 파일 시스템을 사용하고, `prod` 프로필은 S3를 사용합니다.

입양 게시글, 댓글, 사용자 프로필, 채팅 첨부파일은 직접 구현체를 호출하지 않고 `StorageService`를 통해 파일 작업을 수행합니다.

업로드 후 DB 작업이 실패하면 서비스 계층에서 업로드된 파일 삭제를 시도합니다. 파일 삭제 이벤트는 `storage.event` 패키지에서 처리합니다.

## Validation

입력값 정책의 기준은 `global.validation.ValidationPolicy`와 `src/main/resources/validation-policy.json`입니다. 사람이 읽는 문서는 [검증 정책](validation-policy.md)에 있습니다.

프론트엔드는 validation policy JSON 사본을 사용하고 동기화 검사를 수행합니다.

## 관측성

Actuator는 health, metrics, prometheus endpoint를 노출하도록 설정되어 있습니다. 부하 테스트 관측 구성은 [부하 테스트 문서](../load-test/README.md)를 기준으로 합니다.
