# Architecture

백엔드 다이어그램
[backend-architecture.mmd](backend-architecture.mmd)

다이어그램의 실선은 주요 요청·호출·메시지 전달, 점선은 이벤트 발행과 설정에 따른 구현체 선택을 나타냅니다.

일반적인 응답과 일부 서비스 간 호출은 생략했으며, 공통 처리·운영 지원은 여러 계층에서 사용하는 기능입니다.

## 패키지 구조

| 패키지 | 책임 |
| --- | --- |
| `adoption` | 입양 게시글, 댓글, 이미지, 입양 완료 처리 |
| `auth` | 로컬 로그인, OAuth2, JWT, CSRF, security filter chain |
| `chat` | 채팅방, 메시지, 첨부파일, WebSocket/STOMP |
| `storage` | 설정 기반 파일 업로드, 삭제, 접근 URL 생성 |
| `user` | 사용자 계정, 프로필, 계정 상태 |
| `global` | ProblemDetail 오류 처리, 설정, validation, TSID 변환 |
| `audit` | 보안/인가 관련 감사 로그 |

## 계층 규칙

일반 흐름은 아래 순서입니다.

```text
Controller -> Service -> Repository -> Entity
```

Controller는 HTTP 요청/응답, 인증 principal, validation을 다룹니다.

Service는 도메인 규칙과 트랜잭션을 담당합니다.

Repository는 Spring Data JPA query를 담당합니다.

## HTTP 응답과 예외

일반 API의 성공 응답은 공통 래퍼 없이 DTO를 직접 반환합니다. 반환할 데이터가 없는 성공 응답은 `204 No Content`를 사용합니다. 오류 응답의 필드와 JSON 예시는 [API 가이드](api-guide.md)를 봅니다.

커스텀 예외는 `BusinessException`을 상속하고 `ErrorCode`를 가집니다.

MVC에서 처리되는 비즈니스 예외, validation 예외, 인증/인가 예외, 미처리 예외는 `GlobalExceptionHandler`가 `ProblemDetail`로 변환합니다.

필터에서 발생한 JWT·CSRF 오류와 Security의 인증·인가 실패도 같은 `ProblemDetail` 형식으로 작성합니다. 로그아웃 성공은 `CustomLogoutSuccessHandler`가 `204 No Content`로 반환합니다.

STOMP는 HTTP와 별도의 오류 처리 경로를 사용합니다. `@MessageMapping` 처리 중 발생한 비즈니스·validation·인가·미처리 예외는 `StompExceptionHandler`가 `StompErrorResponse`로 변환하여 오류가 발생한 세션의 `/user/queue/errors`로 전송하고 연결을 유지합니다. CONNECT 인증/CSRF, 허용되지 않은 destination, STOMP 프로토콜 오류처럼 현재 연결을 계속 사용할 수 없는 오류는 `StompProtocolErrorHandler`가 같은 payload를 `ERROR` 프레임으로 반환하며, 이 경우 STOMP 연결은 종료됩니다. 두 응답은 각각 `fatal=false`, `fatal=true`로 구분합니다.

## 인증·인가 구조

### 공통 원칙

HTTP와 WebSocket/STOMP는 `JwtAuthenticationService`를 통해 JWT를 검증하고 인증 객체를 생성합니다. 인증은 사용자를 식별하는 단계이며, 실제 작업의 허용 여부는 요청·메시지별 권한 검사와 서비스의 소유권·참여자 검사에서 판단합니다.

상세 토큰 흐름과 실패 응답은 [인증 흐름](auth-flow.md)을 봅니다.

### HTTP

#### Security filter chain

| 우선순위 | 필터 체인                     | 대상           | 설정 |
| --- |---------------------------|--------------| --- |
| 1 | oauth2SecurityFilterChain | `/oauth2/**` | OAuth2 로그인과 callback |
| 2 | commonSecurityFilterChain |  OAuth2 외 전체 | CORS, custom CSRF filter, JWT 인증, 요청 인가, logout |

일반 HTTP 보안 체인은 명시적으로 허용한 요청 외에는 인증을 요구합니다. 컨트롤러의 `@PreAuthorize`와 서비스의 권한 검사는 추가로 적용됩니다. 필터 체인에서 거부한 요청을 컨트롤러에서 다시 허용할 수는 없습니다.

#### JWT 토큰 검사

`JwtAuthenticationFilter`는 `CommonSecurityConfig`의 보안 체인에 `UsernamePasswordAuthenticationFilter`보다 앞선 위치로 등록됩니다. `Authorization: Bearer ...` 헤더를 읽고 `JwtAuthenticationService`를 통해 인증 객체를 만들어 `SecurityContext`에 저장합니다.

#### HTTP CSRF 토큰 검사

HTTP CSRF 검사는 `POST`, `PUT`, `PATCH`, `DELETE` 요청에 `refresh_token` 또는 `recent_auth` 쿠키가 있을 때 적용됩니다. `/ws`와 `/ws/**`는 HTTP CSRF 검사에서 제외하고 STOMP 단계에서 검사합니다.

### WebSocket/STOMP

#### Handshake와 CONNECT

WebSocket에서는 HTTP handshake에서 CSRF 쿠키를 세션 속성에 보관하고, STOMP `CONNECT` 단계에서 CSRF 헤더와 JWT를 검증합니다. 둘 중 하나라도 없거나 유효하지 않으면 연결을 거부합니다. 인증 정보와 검증된 JWT의 만료 시각을 연결에 보관하며, `WebSocketTokenSessions`가 만료 시 수신 전용 연결까지 종료합니다. 프론트엔드는 토큰을 갱신한 뒤 재연결·재구독하고, 갱신 실패 시 재연결을 중단하고 로그인 화면으로 이동합니다. HTTP handshake 통과와 STOMP 메시지 전송·구독 권한은 별개의 검사입니다.

#### SEND와 SUBSCRIBE

`SEND`와 `SUBSCRIBE`에서는 저장된 인증 정보, CSRF 검증 완료 여부와 JWT 만료 시각을 확인하며 JWT 서명을 다시 검증하지 않습니다. `SEND`는 `/pub/chat/message`만 허용하고, `SUBSCRIBE`는 `/sub/chat/room/{roomId}`와 세션 전용 `/user/queue/errors`만 허용합니다. 클라이언트의 `/sub/**` 직접 전송과 `/queue/errors` 직접 구독을 포함한 그 외 목적지는 차단합니다. 채팅방 구독은 `WebSocketConfig`가 인증 사용자와 참여 여부를 `RoomParticipantRepository`로 직접 확인합니다. 텍스트 메시지 전송은 `ChatWebSocketController`의 `@PreAuthorize`와 `ChatService`의 전송 권한 검사를 거칩니다.

## 실시간 채팅 구조

`chat.config.WebSocketConfig`가 STOMP endpoint와 broker prefix를 설정합니다.

- endpoint: `/ws`
- application destination prefix: `/pub`
- simple broker prefix: `/sub`, `/queue`
- user destination prefix: `/user`
- 채팅 메시지 publish: `/pub/chat/message`
- 채팅방 구독: `/sub/chat/room/{roomId}`
- 세션 전용 오류 구독: `/user/queue/errors`

REST 채팅 API는 `ChatController`, STOMP 텍스트 메시지는 `ChatWebSocketController`가 처리합니다. 첨부파일 메시지와 텍스트 메시지는 각각 서비스 저장 성공 후 `SimpMessagingTemplate`으로 발행됩니다. 프로세스 내 Simple Broker가 `/sub/chat/room/{roomId}` 구독자에게 전달합니다.

## 저장소 구조

`storage.service.StorageService`는 파일 업로드, 삭제 및 접근 URL 생성을 담당하는 저장소 추상화입니다.

저장소 구현체는 `app.storage.type=local|s3` 설정에 따라 선택됩니다.

* `local`: 로컬 파일 시스템 사용
* `s3`: Amazon S3 사용

일반적으로 로컬 환경에서는 `local`, 운영 환경에서는 `s3` 구현체를 사용합니다.

### 파일 접근

#### Local Storage

로컬 저장소에서는 `LocalStorageService`가 `LocalStorageUrlSigner`를 사용해 만료 시각과 HMAC-SHA256 서명이 포함된 조회 또는 다운로드 URL을 생성합니다.

클라이언트가 `/api/v1/storage/files/**`로 파일을 요청하면 `LocalStorageController`가 동일한 `LocalStorageUrlSigner`를 사용해 다음 값을 검증합니다.

* `expires`
* `signature`

서명 검증에 성공하면 로컬 파일을 응답합니다.

서명 대상에는 다음 값이 포함됩니다.

* HTTP method (`GET`)
* 파일 경로
* 만료 시각
* 다운로드 파일명

일반 조회 URL에서는 다운로드 파일명 항목에 빈 문자열을 사용합니다.

#### S3

S3 저장소에서는 Presigned GET URL을 생성합니다.

브라우저는 이 URL을 이용해 백엔드를 거치지 않고 S3에서 파일을 직접 조회하거나 다운로드합니다.

즉, 파일 조회 경로는 저장소 종류에 따라 다음과 같이 달라집니다.

```text
Local
Browser
  → Backend (/api/v1/storage/files/**)
  → Local File System

S3
Browser
  → S3 (Presigned GET URL)
```

### 파일 업로드

파일 업로드는 저장소 종류와 관계없이 REST Multipart 요청으로 백엔드에 전달됩니다.

```text
Browser
  → Backend
  → StorageService
  → Local File System / S3
```

백엔드는 `StorageService`를 통해 실제 저장소 구현체에 파일 저장을 위임합니다.

## 파일 삭제 및 보상 처리

DB 트랜잭션과 파일 저장소는 하나의 트랜잭션으로 묶을 수 없기 때문에, 파일 업로드 이후 DB 처리 결과에 따라 이벤트와 직접 삭제를 이용해 파일을 정리합니다.

### 기존 파일 삭제

프로필 이미지 등의 파일을 새 파일로 교체하는 경우, 기존 파일은 DB 변경이 성공적으로 커밋된 이후 삭제해야 합니다.

이를 위해 DB 변경 트랜잭션 안에서 `DeleteOldStorageFilesEvent`를 발행하고, `StorageEventListener`가 커밋 후 `AFTER_COMMIT` 시점에 이벤트를 처리합니다.

실제 파일 삭제는 `storageTaskExecutor`를 통해 비동기로 수행됩니다.

```text
새 파일 저장
  → DB 변경
  → DeleteOldStorageFilesEvent 발행
  → Transaction COMMIT
  → AFTER_COMMIT
  → 기존 파일 비동기 삭제
```

DB 변경이 롤백된 경우에는 기존 파일을 유지합니다.

### 새로 업로드한 파일 정리

파일 업로드 이후 DB 변경이 실패하면 이미 저장소에 업로드된 새 파일을 제거해야 합니다.

이를 위해 트랜잭션 내부에서 `DeleteUploadedStorageFilesEvent`를 발행합니다.

`StorageEventListener`는 해당 이벤트를 `AFTER_ROLLBACK` 시점에 처리하여 새로 업로드된 파일의 삭제를 비동기로 시도합니다.

```text
새 파일 저장
  → DeleteUploadedStorageFilesEvent 발행
  → DB 변경
       ├─ COMMIT   → 새 파일 유지
       └─ ROLLBACK → AFTER_ROLLBACK
                    → 새 파일 비동기 삭제
```

입양 서비스에서는 `TransactionTemplate` 콜백 내부에서 이벤트를 발행합니다.

따라서 해당 로직이 기존 트랜잭션에 참여한 상태에서 실행되고 이후 상위 트랜잭션이 롤백되는 경우에도 `AFTER_ROLLBACK` 이벤트를 통해 업로드된 파일을 정리할 수 있습니다.

### 트랜잭션 외부 실패 처리

모든 실패가 트랜잭션 롤백 이벤트로 처리되는 것은 아닙니다.

서비스 내부에서 예외를 포착한 경우에는 이벤트를 기다리지 않고 `StorageService`를 직접 호출하여 업로드된 파일의 삭제를 시도합니다.

이 방식은 트랜잭션이 시작되기 전에 실패한 경우도 처리할 수 있습니다.

경우에 따라 직접 삭제와 `AFTER_ROLLBACK` 삭제가 동일한 파일에 대해 모두 실행될 수 있습니다.

이러한 중복 삭제는 허용합니다.

* Local Storage: `deleteIfExists`
* S3: 객체 삭제 요청

이미 삭제되어 파일이 존재하지 않더라도 오류로 취급하지 않기 때문에 삭제 작업은 멱등적으로 수행할 수 있습니다.

## 일관성 보장 범위

DB와 파일 저장소 간의 원자적 처리를 보장하는 구조는 아닙니다.

DB 트랜잭션과 Local File System 또는 S3 작업은 서로 다른 시스템에서 수행되기 때문에 다음과 같은 상태가 발생할 수 있습니다.

```text
DB 변경 성공
파일 삭제 실패
    ↓
DB에서는 더 이상 참조하지 않지만
저장소에는 파일이 남음
    ↓
고아 파일(orphan file)
```

파일 삭제 실패는 로그에 기록합니다.

삭제 실패로 인해 참조를 잃은 고아 파일은 별도의 운영 작업이나 정리 스크립트를 통해 삭제해야 합니다.


## Validation

런타임 입력 검증은 `global.validation.ValidationPolicy`의 Java 상수를 사용합니다. `src/main/resources/validation-policy.json`은 프론트엔드와 백엔드 사이의 공유 계약 파일이며 `ValidationPolicyContractTest`가 Java 정책과의 일치를 확인합니다. 자세한 내용은 [검증 정책](validation-policy.md)에 있습니다.

프론트엔드는 validation policy JSON 사본을 사용하고 동기화 검사를 수행합니다.

## 관측성

Actuator는 health, metrics, prometheus endpoint를 노출하도록 설정되어 있습니다. 부하 테스트 관측 구성은 [부하 테스트 문서](load-test.md)를 기준으로 합니다.
