# 오류 처리 정책

## 책임 경계

- 도메인 오류 enum은 `ErrorSpec`을 구현하고 오류 코드, 공개 메시지, 의미 분류만 소유한다.
- 도메인 및 유스케이스 코드는 HTTP 상태나 STOMP 프레임을 참조하지 않는다.
- HTTP 상태는 `HttpErrorPolicy`, HTTP 본문은 `ProblemDetailFactory`가 결정한다.
- Security 필터처럼 DispatcherServlet 밖에서 발생한 오류는 `ProblemDetailWriter`를 사용한다.
- STOMP는 동일한 `ErrorSpec`을 사용하되 연결 유지 여부와 프레임 형식은 STOMP 경계에서 결정한다.
- OAuth2 redirect에는 `OAuth2FailureMapper`에 등록된 공개 오류만 포함한다.

## 공개 계약

- `errorCode`가 클라이언트 분기의 기준이다. 공개된 코드는 다른 의미로 재사용하지 않는다.
- `detail`은 사용자에게 노출 가능한 기본 메시지다. 예외의 `getMessage()`나 외부 시스템 description을 응답에 사용하지 않는다.
- HTTP 오류는 `application/problem+json`으로 반환한다.
- HTTP 응답에는 서버가 생성한 `requestId`를 포함하며 같은 값은 `X-Request-Id` 헤더와 로그 MDC에 기록한다.
- validation 오류는 `location`, `field`, `message`를 사용하고 rejected value는 노출하지 않는다.

## 오류 의미와 HTTP 상태

| FailureKind | HTTP 상태 |
|---|---:|
| INVALID_INPUT | 400 |
| UNAUTHENTICATED | 401 |
| FORBIDDEN | 403 |
| NOT_FOUND | 404 |
| CONFLICT | 409 |
| GONE | 410 |
| LIMIT_EXCEEDED | 413 |
| DEPENDENCY_FAILURE | 500 |
| INTERNAL | 500 |

`AUTH_INVALID_CREDENTIALS`는 401, 입양 상태 충돌 및 삭제된 댓글 조작은 409를 반환한다. 로그인 자격 증명 실패는 토큰 갱신 대상이 아니다.

## 로그 정책

- `INTERNAL`, `DEPENDENCY_FAILURE`는 경계에서 stack trace와 함께 error로 한 번 기록한다.
- 예상 가능한 4xx 오류는 경계에서 debug로 기록한다.
- 보안 및 계정 상태 변경은 일반 오류 로그와 별도로 `AuditLogger`를 사용한다.
- 토큰, 쿠키, 요청 본문, 외부 provider 원문은 로그나 redirect에 기록하지 않는다.
- 경계에서 기록될 예외를 서비스에서 다시 error로 기록하지 않는다. 단, 삼킨 보상 작업 실패는 발생 지점에서 기록한다.

## 스토리지 삭제

현재 삭제 계약은 명시적인 best-effort다. `*BestEffort` 메서드는 삭제 실패를 로그로 남기고 호출자에게 전파하지 않는다. 개인정보 삭제나 orphan 파일 방지가 강한 요구사항이 되면 메서드의 예외 계약만 강화하지 말고 outbox, 재시도 및 실패 저장소를 함께 도입한다.

## 새 오류 추가 절차

1. 오류를 소유한 도메인의 enum에 `ErrorSpec`을 추가한다.
2. 기존 의미로 표현할 수 없는 경우에만 `FailureKind`와 경계 정책을 확장한다.
3. 도메인 예외의 의미 있는 factory를 통해 오류를 발생시킨다.
4. `ErrorCatalogTest`의 전역 코드 유일성 검사를 통과시킨다.
5. 클라이언트가 해당 코드로 분기해야 할 때만 프론트 `ErrorCodes`에 추가한다.

## 확정된 정책

- 로그인 자격 증명 실패는 401을 반환한다.
- 삭제된 입양 게시글은 410으로 공개하고 프론트는 '삭제된 게시물입니다' 화면을 표시한다.
- 입양 상태 충돌과 이미 삭제된 댓글 조작은 409를 반환한다.
- 현재 규모와 중요도를 고려해 스토리지 삭제는 best-effort를 유지한다.
- MySQL duplicate key(1062)는 409로 변환한다. 이름이 알려진 unique constraint는 도메인 오류에 매핑하고 미등록 이름은 `RESOURCE_CONFLICT`로 반환한다. 다른 무결성 위반은 500으로 유지한다.
- 기존 DB는 `docs/migrations/2026-10-04-error-policy-unique-constraints.sql`을 수동 적용한다. 자동 생성 index 이름을 표준 이름으로 변경하고 중복 index를 정리한다. 적용 전 백업 및 쓰기 중단이 필요하며 DDL은 자동 커밋된다. 실패 시 schema와 남은 procedure를 확인한 후 재개한다.
- 서버 한국어 기본 메시지는 유지한다. 프론트 `src/global/i18n/errorMessages.ts`에서 오류 코드별 언어 사전을 관리하며, 선택 언어의 번역 → 서버 공개 메시지 → 호출부 기본 메시지 순서로 표시한다. 기본 언어는 한국어이며 언어 추가 시 사전 및 locale 인자를 확장한다. 필드별 validation 메시지는 기존 계약을 유지한다.
