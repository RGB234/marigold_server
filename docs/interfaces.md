# 인터페이스

## 기준

REST API의 공통 prefix는 `/api/v1`입니다. 상세 경로, 요청 필드와 응답 스키마는 로컬 프로필의 Swagger/OpenAPI를 기준으로 하며, 이 문서는 인터페이스 묶음과 공통 계약만 설명합니다.

## REST 영역

- `/auth`: 로컬 가입·로그인, 인증 상태, token refresh와 로그아웃
- `/user`: 공개 프로필·검색과 인증 사용자의 프로필·계정 관리
- `/adoption`: 게시글·댓글 조회와 작성, 입양자 확정
- `/chat/rooms`: 채팅방, 메시지 이력, 첨부파일과 방 나가기
- `/storage`: 로컬 저장소에서만 사용하는 서명된 파일 조회·다운로드

`/storage`는 업로드용 공용 API가 아니며 S3를 사용하는 운영 프로필에서는 활성화되지 않습니다.

## 인증과 CSRF

- Access token은 응답 본문으로 발급하고 이후 `Authorization: Bearer {token}`으로 전달합니다.
- Refresh token은 HttpOnly `refresh_token` 쿠키로 발급합니다.
- Access token의 기본 유효시간은 10분입니다. REST와 WebSocket 인증은 서명, 만료, token 용도 (access or refresh) 와 claim을 검증합니다.
- 사용자 상태나 권한 변경은 기존 access token 만료까지 최대 10분 지연될 수 있습니다. (Bounded staleness)
- CSRF token은 인증 세션에 HMAC-SHA256으로 결속한 signed double-submit token이며, 읽을 수 있는 `XSRF-TOKEN` 쿠키와 `X-CSRF-TOKEN` 응답 헤더로 발급합니다.
- `POST`, `PUT`, `PATCH`, `DELETE` 요청에 refresh 또는 recent-auth 쿠키가 포함되면 쿠키와 `X-CSRF-TOKEN` 요청 헤더가 일치하고 서명이 현재 인증 세션에 유효해야 합니다.
- 자격 증명 변경이나 회원 탈퇴 같은 민감 작업은 access token과 최근 인증을 함께 요구합니다. Recent-auth의 기본 유효시간은 5분입니다.

로컬 로그인은 access token을 응답 본문에 담고 refresh, CSRF, recent-auth 쿠키를 함께 발급합니다. Refresh 요청은 refresh token을 검증한 뒤 DB에서 사용자 존재 여부, 상태와 최신 권한을 확인하고, 정상 사용자에게만 access와 refresh token을 재발급합니다.

Kakao/Naver OAuth2는 `/oauth2/authorization/{provider}`에서 시작하고 `/oauth2/code/{provider}`로 돌아옵니다. 성공 시 access token을 URL에 싣지 않고 쿠키를 발급한 뒤 프론트엔드 callback으로 redirect합니다. 프론트엔드는 이후 refresh 흐름으로 access token을 얻습니다.

로그아웃은 컨트롤러가 아니라 Spring Security logout filter가 `POST /api/v1/auth/logout`을 처리합니다.

## WebSocket/STOMP

채팅은 SockJS endpoint `/ws`를 사용합니다.

- CONNECT native header: `Authorization: Bearer {token}`, `X-CSRF-TOKEN: {token}`
- SEND: `/pub/chat/message`
- SUBSCRIBE: `/sub/chat/room/{roomId}`
- 사용자 오류 queue: `/user/queue/errors`

CONNECT의 CSRF header는 handshake 때 전달된 `XSRF-TOKEN` 쿠키와 일치하고 현재 인증 세션에 유효한 서명을 가져야 합니다. 방 구독은 해당 방 참여자에게만 허용됩니다. REST multipart로 첨부 메시지를 저장하면 같은 방 구독 경로로 메시지를 발행합니다.

메시지 처리 중 복구 가능한 오류는 사용자 오류 queue로 전달합니다. CONNECT나 SUBSCRIBE 단계의 치명적 오류는 STOMP `ERROR` frame으로 응답하며, access token 만료 시 연결을 닫습니다.

## HTTP 오류

HTTP 오류는 `application/problem+json` 형식의 Problem Details 응답을 사용합니다.

```json
{
  "type": "urn:marigold:error:ERROR_CODE",
  "title": "Bad Request",
  "status": 400,
  "detail": "사용자에게 보여 줄 메시지",
  "instance": "/api/v1/example",
  "errorCode": "ERROR_CODE",
  "requestId": "server-generated-id"
}
```

입력 검증 실패에는 `errors` 목록이 추가됩니다. 모든 HTTP 응답에는 `X-Request-Id`가 포함되며 오류 응답의 `requestId`와 서버 로그를 연결하는 데 사용합니다.

OAuth2 실패는 Problem Details 대신 프론트엔드 callback의 query parameter로 전달되고, STOMP 오류는 별도의 메시지 형식을 사용합니다.
