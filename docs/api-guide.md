# API Guide

이 문서는 REST API를 사용할 때 필요한 공통 규칙만 다룹니다. 엔드포인트별 상세 요청/응답은 Swagger UI와 OpenAPI JSON을 기준으로 확인합니다.

## API 문서 소스

API 상세 명세는 컨트롤러와 DTO의 OpenAPI 애노테이션이 기준입니다.

```powershell
cd back
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

기본 설정과 운영 설정에서는 `springdoc`이 비활성화되어 있습니다. `local` 프로필에서는 활성화됩니다.

## Base Path

REST API prefix는 `/api/v1`입니다.

| 도메인 | Base path | 설명 |
| --- | --- | --- |
| Auth | `/api/v1/auth` | 회원가입, 로그인, 인증 상태, token refresh |
| User | `/api/v1/user` | 사용자 계정, 프로필, 보안 정보 |
| Adoption | `/api/v1/adoption` | 입양 게시글, 댓글, 입양 완료 처리 |
| Chat | `/api/v1/chat` | 채팅방, 메시지, 첨부파일 URL |

## HTTP 응답

성공 응답은 공통 래퍼 없이 리소스 DTO를 JSON 본문으로 직접 반환합니다. 생성 API는 `201 Created`와 `Location` 헤더를 사용하고, 반환할 데이터가 없는 성공 응답은 `204 No Content`를 사용합니다.

조회 성공 예시:

```json
{
  "id": "01JABCDEF1234",
  "nickname": "marigold"
}
```

오류 응답은 RFC 9457 `ProblemDetail` 형식과 `application/problem+json` Content-Type을 사용합니다. `errorCode`는 클라이언트 분기용 애플리케이션 오류 코드이고, validation 오류가 있으면 `errors` 확장 필드를 포함합니다.

```json
{
  "type": "urn:marigold:error:INVALID_INPUT_VALUE",
  "title": "Bad Request",
  "status": 400,
  "detail": "입력값이 올바르지 않습니다.",
  "errorCode": "INVALID_INPUT_VALUE",
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

`errors`는 필드 오류가 없으면 생략됩니다. HTTP 상태는 응답 본문의 `status`가 아니라 실제 응답 상태를 기준으로 처리합니다.

## HTTP 응답 형식 예외

DTO 또는 `ProblemDetail` JSON을 사용하지 않는 주요 응답은 다음과 같습니다. 아래 표는 정상 응답 또는 해당 프로토콜의 응답 형식을 설명합니다.

| 구분                         | 경로·대상 | 응답 형식 |
|----------------------------| --- | --- |
| 로컬 파일 조회 (profile=local)   | `GET /api/v1/storage/files/{domain}/{type}/{fileName}` | `ResponseEntity<Resource>`로 파일 본문을 직접 반환. 파일의 `Content-Type`, `Content-Length` 설정 |
| 로컬 파일 다운로드 (profile=local) | 위 경로의 `/download` | 파일 본문을 직접 반환하고 `Content-Disposition: attachment`로 다운로드 파일명 지정 |
| OAuth2 로그인 시작              | `/oauth2/authorization/kakao`, `/oauth2/authorization/naver` | 공급자 로그인 페이지로 HTTP redirect |
| OAuth2 callback 처리 결과      | `/oauth2/code/kakao`, `/oauth2/code/naver` | 성공·실패 핸들러가 프론트 callback으로 redirect. 결과는 `auth_status` 또는 `error`, `error_description` query로 전달 |
| 실시간 채팅 메시지                 | `/sub/chat/room/{roomId}` 구독 | STOMP 메시지 payload로 `ChatMessageDto`를 직접 전달. 텍스트 메시지와 첨부파일 메시지의 구독 알림 모두 해당 |
| 복구 가능한 STOMP 오류             | `/user/queue/errors` 구독 | 오류를 발생시킨 세션에 `StompErrorResponse`를 전달하고 연결 유지 |
| 치명적인 STOMP 오류               | STOMP `ERROR` 프레임 | `StompErrorResponse`를 전달한 뒤 연결 종료 |
| WebSocket·SockJS 연결        | `/ws`, `/ws/**` | 연결 handshake와 SockJS/STOMP 프로토콜 응답 |
| 상태·메트릭 조회                  | `/actuator/health`, `/actuator/metrics`, `/actuator/metrics/{name}` | Actuator 자체 JSON 형식 |
| Prometheus 수집              | `/actuator/prometheus` | 메트릭 수집용 텍스트 형식 |
| API 명세·문서 화면               | `/v3/api-docs`, `/swagger-ui/**` | OpenAPI 명세 JSON과 Swagger UI의 HTML·정적 리소스. 현재 local 프로필에서 활성화 |

S3 presigned URL로 접근한 파일은 S3가 직접 응답하므로 백엔드 HTTP 응답 형식의 적용 대상이 아닙니다.

## STOMP 오류 응답

STOMP 오류는 HTTP 상태를 갖지 않으므로 `ProblemDetail` 대신 다음과 같은 형태의 `StompErrorResponse` payload를 사용합니다.

```json
{
  "timestamp": "2026-09-25T12:34:56",
  "errorCode": "INVALID_INPUT_VALUE",
  "message": "입력값이 올바르지 않습니다.",
  "fatal": false,
  "command": "SEND",
  "destination": "/pub/chat/message",
  "errors": [
    {
      "field": "message",
      "message": "메시지를 입력해주세요."
    }
  ]
}
```

`command`, `destination`, `errors`는 값이 없으면 생략됩니다. `fatal=false`는 `/user/queue/errors`로 전달되는 복구 가능한 메시지 오류이고, `fatal=true`는 현재 연결을 종료하는 STOMP `ERROR` 프레임입니다. `ERROR` 프레임은 동일한 값을 `error-code` native header에도 포함합니다.
