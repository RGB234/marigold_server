# Architecture

이 문서는 코드 위치와 책임을 빠르게 찾기 위한 안내서입니다.

전체 백엔드 구성도는 [backend-architecture.mmd](backend-architecture.mmd)에 있습니다.

다이어그램의 실선은 주요 요청·호출·메시지 전달, 점선은 이벤트 발행과 설정에 따른 구현체 선택을 나타냅니다. 일반적인 응답과 일부 서비스 간 호출은 생략했으며, 공통 처리·운영 지원은 여러 계층에서 사용하는 기능입니다.

## 패키지 구조

| 패키지 | 책임 |
| --- | --- |
| `adoption` | 입양 게시글, 댓글, 이미지, 입양 완료 처리 |
| `auth` | 로컬 로그인, OAuth2, JWT, CSRF, security filter chain |
| `chat` | 채팅방, 메시지, 첨부파일, WebSocket/STOMP |
| `storage` | 설정 기반 파일 업로드, 삭제, 접근 URL 생성 |
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
| 2 | OAuth2 외 전체 | CORS, custom CSRF filter, logout |

필터 체인 레벨에서는 대부분의 요청을 통과시키고, 실제 API 접근 권한은 컨트롤러의 `@PreAuthorize`에서 판단합니다.

`JwtAuthenticationFilter`는 현재 `CommonSecurityConfig`의 체인 내부에 명시적으로 추가되지 않은 별도 `@Component` Servlet Filter입니다. `Authorization: Bearer ...` 헤더를 읽고 `JwtAuthenticationService`를 통해 인증 객체를 만들어 `SecurityContext`에 저장합니다. 다이어그램의 일반 HTTP 보안 노드는 이 구성을 묶어 표현하며, 내부 필터 순서를 나타내지는 않습니다.

HTTP CSRF 검사는 `POST`, `PUT`, `PATCH`, `DELETE` 요청에 `refresh_token` 또는 `recent_auth` 쿠키가 있을 때 적용됩니다. `/ws`와 `/ws/**`는 HTTP CSRF 검사에서 제외하고 STOMP 단계에서 검사합니다.

## WebSocket 구조

`chat.config.WebSocketConfig`가 STOMP endpoint와 broker prefix를 설정합니다.

- endpoint: `/ws`
- application destination prefix: `/pub`
- simple broker prefix: `/sub`
- 채팅 메시지 publish: `/pub/chat/message`
- 채팅방 구독: `/sub/chat/room/{roomId}`

WebSocket에서는 handshake에서 CSRF 쿠키를 세션 속성에 보관하고, STOMP `CONNECT` 단계에서 CSRF 헤더와 비교한 뒤 JWT 인증을 시도합니다. JWT가 없거나 유효하지 않다는 이유만으로 `CONNECT`를 거부하지는 않습니다. `SEND`와 `SUBSCRIBE`에서는 해당 연결의 CSRF 검증 완료 여부를 확인합니다.

채팅방 구독은 `WebSocketConfig`가 인증 사용자와 참여 여부를 `RoomParticipantRepository`로 직접 확인합니다. 텍스트 메시지 전송은 `ChatController`의 `@PreAuthorize`와 `ChatService`의 전송 권한 검사를 거칩니다.

텍스트 메시지는 `ChatController`, 첨부파일 메시지는 REST의 `ChatRoomController`가 서비스 저장 성공 후 `SimpMessagingTemplate`으로 발행합니다. 프로세스 내 Simple Broker가 `/sub/chat/room/{roomId}` 구독자에게 전달합니다.

## 저장소 구조

`storage.service.StorageService`가 파일 업로드, 삭제, 접근 URL 생성을 담당합니다. `local` 프로필은 로컬 파일 시스템을 사용하고, `prod` 프로필은 S3를 사용합니다.

구성도의 `FS`는 설정에 따라 사용하는 로컬 파일 시스템 또는 S3를 하나로 표현한 논리적 파일 저장소입니다. 각 화살표의 `app.storage.type` 라벨로 적용 조건을 구분합니다.

저장소 구현체는 `app.storage.type=local|s3` 설정으로 선택합니다. 로컬 저장소는 `LocalStorageService`가 `LocalStorageUrlSigner`로 만료 시각과 HMAC-SHA256 서명을 포함한 조회/다운로드 URL을 생성합니다. `LocalStorageController`도 같은 서명기를 사용해 `/api/v1/storage/files/**` 요청의 `expires`, `signature`를 검증한 뒤 로컬 파일을 응답합니다. 서명 대상은 `GET`, 경로, 만료 시각, 다운로드 파일명이며 조회 URL의 파일명 항목은 빈 문자열입니다.

브라우저는 로컬 서명 URL로 백엔드에 파일을 요청하거나, S3 Presigned GET URL로 S3에 직접 요청합니다. 파일 업로드는 REST Multipart 요청으로 백엔드와 `StorageService`를 거칩니다.

입양 게시글, 댓글, 사용자 프로필, 채팅 첨부파일은 직접 구현체를 호출하지 않고 `StorageService`를 통해 파일 작업을 수행합니다.

기존 파일 삭제는 사용자·입양 서비스가 발행한 `DeleteOldStorageFilesEvent`를 `StorageEventListener`가 `AFTER_COMMIT` 시점에 `storageTaskExecutor`로 비동기 처리합니다.

사용자 프로필, 입양 게시글·댓글 이미지, 채팅 첨부파일을 새로 업로드하면 DB 변경 전 트랜잭션 안에서 `DeleteUploadedStorageFilesEvent`를 발행합니다. 리스너는 `AFTER_ROLLBACK` 시점에 새 파일 삭제를 비동기로 시도하며, 커밋되면 새 파일을 유지합니다. 입양 서비스는 `TransactionTemplate` 콜백 안에서 이벤트를 발행하므로, 해당 서비스가 기존 트랜잭션에 참여했다가 나중에 롤백되는 경우에도 정리됩니다.

서비스 내부에서 포착한 실패는 기존처럼 `StorageService`를 직접 호출해 즉시 삭제도 시도합니다. 이는 트랜잭션 시작 전 실패에도 적용됩니다. 롤백 이벤트와 직접 삭제가 겹칠 수 있으며, 로컬의 `deleteIfExists`와 S3 객체 삭제는 같은 파일의 중복 삭제를 허용합니다. 파일 삭제 실패는 로그에 기록되므로 DB와 파일 저장소의 원자적 처리를 보장하는 구조는 아닙니다.

## Validation

런타임 입력 검증은 `global.validation.ValidationPolicy`의 Java 상수를 사용합니다. `src/main/resources/validation-policy.json`은 공유 계약 파일이며 `ValidationPolicyContractTest`가 Java 정책과의 일치를 확인합니다. 사람이 읽는 문서는 [검증 정책](validation-policy.md)에 있습니다.

프론트엔드는 validation policy JSON 사본을 사용하고 동기화 검사를 수행합니다.

## 관측성

Actuator는 health, metrics, prometheus endpoint를 노출하도록 설정되어 있습니다. 부하 테스트 관측 구성은 [부하 테스트 문서](../load-test/README.md)를 기준으로 합니다.
