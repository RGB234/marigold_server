# 인터페이스

## 기준

REST API의 공통 prefix는 `/api/v1`입니다. 상세 경로, 요청 필드와 응답 스키마는 로컬 프로필의 Swagger/OpenAPI를 기준으로 하며, 이 문서는 인터페이스 묶음과 공통 계약만 설명합니다.

## REST 영역

- `/auth`: 로컬 가입·로그인, 인증 상태, token refresh와 로그아웃
- `/user`: 공개 프로필·검색과 인증 사용자의 프로필·계정 관리
- `/adoption`: 게시글·댓글 조회와 작성, 입양자 확정
- `/chat/rooms`: 채팅방, 메시지 이력, 첨부파일과 방 나가기
- `/storage`: 업로드 URL 발급과, 로컬 저장소에서만 사용하는 서명된 파일 조회·다운로드·업로드

## 파일 업로드

게시글·댓글·프로필·채팅 첨부 API는 multipart를 받지 않습니다. 파일은 다음 순서로 보냅니다.

1. `POST /storage/uploads`에 `purpose`(`ADOPTION_POST_IMAGE`, `ADOPTION_COMMENT_IMAGE`, `USER_PROFILE_IMAGE`,
   `CHAT_ATTACHMENT`)와 파일별 `fileName`, `size`를 보냅니다. 응답은 요청 순서대로 `uploadUrl`, `contentType`, `uploadToken`입니다.
2. 각 파일을 `uploadUrl`로 `PUT`합니다. `Content-Type` 헤더에는 응답의 `contentType`을 그대로 넣습니다. 운영에서는 S3 presigned
   URL이라 신고한 크기나 타입과 다르면 S3가 거부하고, 우리 서버의 인증 헤더나 쿠키를 붙이면 안 됩니다.
3. 도메인 API의 JSON 본문에 토큰을 담아 보냅니다. 게시글·댓글·채팅은 `uploadTokens` 목록, 프로필은 `imageUploadToken`입니다.

업로드 URL은 기본 15분, 토큰은 기본 1시간 동안 유효합니다. 토큰이 위조·만료되었거나 다른 사용자·용도의 것이면 `UPLOAD_TOKEN_INVALID`, 파일이 없거나
크기가 다르면 `UPLOAD_INCOMPLETE`로 `400`을 반환합니다. 둘 다 파일을 다시 발급받아 올려야 합니다. `/storage/files/**`는 로컬 프로필에서만
활성화되며, 운영 프로필은 S3 URL을 사용합니다.

## 인증과 CSRF

- Access token은 응답 본문으로 발급하고 이후 `Authorization: Bearer {token}`으로 전달합니다.
- Refresh token은 HttpOnly `refresh_token` 쿠키로 발급합니다.
- REST와 WebSocket 인증은 서명, 만료, token 용도 (access or refresh) 와 claim을 검증합니다.
- 사용자 상태나 권한 변경은 기존 access token 만료까지 최대 10분 지연될 수 있습니다. (Bounded staleness)
- CSRF token은 인증 세션에 HMAC-SHA256으로 결속한 signed double-submit token 방식이며, 읽을 수 있는 `XSRF-TOKEN` 쿠키와 `X-CSRF-TOKEN` 응답 헤더로 발급합니다.
- REST에서 `refresh_token` 또는 `recent_auth` 쿠키가 포함된 POST·PUT·PATCH·DELETE 요청은 `XSRF-TOKEN` 쿠키와 일치하는 `X-CSRF-TOKEN` 요청 헤더를 함께 보내야 합니다.
- Token refresh에 성공하면 refresh token과 CSRF token이 재발급됩니다. 이후 요청에는 새 CSRF token을 사용해야 합니다.
- 자격 증명 변경이나 회원 탈퇴 같은 민감 작업은 access token과 최근 인증을 함께 요구합니다.
- 각 토큰과 최근 인증의 기본 유효시간은 `application.yml`에 명시합니다.

## WebSocket/STOMP

채팅은 SockJS endpoint `/ws`를 사용합니다.

- CONNECT native header: `Authorization: Bearer {token}`, `X-CSRF-TOKEN: {token}`
- SEND: `/pub/chat/message`
- SUBSCRIBE: `/sub/chat/room/{roomId}`
- 사용자 오류 queue: `/user/queue/errors`

구독 경로의 `{roomId}`는 13자리 TSID 문자열입니다.

### 성공 응답

- CONNECT가 성공하면 STOMP `CONNECTED` frame을 받습니다.
- SUBSCRIBE 성공에는 별도 application payload가 없습니다. 이후 `/sub/chat/room/{roomId}`로 수신되는 메시지는 `ChatMessageDto` 형식입니다.
- `/pub/chat/message`로 보낸 메시지가 저장되면 같은 방의 `/sub/chat/room/{roomId}`로 `ChatMessageDto`를 발행합니다. SEND 요청 자체에 대한 별도 application ACK는 없습니다.
- HTTP로 생성한 첨부파일 메시지도 HTTP `201 Created` 응답과 별개로 같은 방 구독 경로에 발행됩니다.

### 실패 응답

메시지 처리 중 발생하여 연결을 계속 사용할 수 있는 오류는 `/user/queue/errors`로 전달합니다.

```json
{
  "timestamp": "2026-10-18T12:34:56",
  "errorCode": "INVALID_INPUT_VALUE",
  "message": "입력값이 올바르지 않습니다.",
  "fatal": false,
  "command": "SEND",
  "destination": "/pub/chat/message",
  "errors": [
    {
      "location": "body",
      "field": "message",
      "message": "메시지를 입력해주세요."
    }
  ]
}
```

`command`, `destination`, `errors`는 해당 정보가 있을 때만 포함됩니다. `fatal: false`이면 현재 연결을 계속 사용할 수 있습니다.

CONNECT, SUBSCRIBE 등 프로토콜 처리 중 연결을 계속 사용할 수 없는 오류는 STOMP `ERROR` frame으로 응답합니다. frame body는 위와 같은 형식이며 `fatal: true`이고, native header `error-code`에도 같은 오류 코드가 포함됩니다. 이 경우 기존 연결을 재사용하지 말고 원인을 해소한 뒤 다시 연결해야 합니다.

연결에 사용한 access token이 만료되면 WebSocket은 close code `4001`, reason `JWT expired`로 종료됩니다. 새 access token을 발급받은 뒤 다시 연결해야 합니다.

## HTTP 응답

### 성공 응답

HTTP 성공은 `2xx` 상태로 판별하며 공통 envelope를 사용하지 않습니다. 일반 API의 응답 본문은 endpoint별 DTO, 목록, page 또는 단일 값이며 정확한 schema는 Swagger/OpenAPI를 따릅니다. `/storage`의 파일 조회·다운로드는 파일 본문을 반환합니다.

- `200 OK`: 조회·변경 결과를 응답 본문으로 반환합니다.
- `201 Created`: 리소스 생성이 완료되었습니다. endpoint에 따라 생성 결과 또는 식별자를 본문으로 반환하거나 빈 본문을 사용하며, 리소스 URI를 제공하는 경우 `Location` header가 포함됩니다.
- `204 No Content`: 작업은 성공했지만 응답 본문이 없습니다.

모든 HTTP 응답에는 서버가 생성한 `X-Request-Id` header가 포함됩니다.

현재 `X-Request-Id`는 CORS 노출 헤더에 포함되지 않아 다른 origin의 프론트엔드 JavaScript에서 직접 읽을 수 없습니다. Problem Details 오류 응답에서는 본문의 `requestId`를 사용할 수 있습니다.

### 실패 응답

애플리케이션의 공통 오류 처리로 반환하는 HTTP 오류는 `application/problem+json` 형식의 Problem Details 응답을 사용합니다. CORS 검사 등 공통 오류 처리에 도달하기 전에 거부되는 요청은 예외이며, 현재 CORS 거부 응답은 `403 Forbidden`과 `Invalid CORS request` 본문을 반환합니다.

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

클라이언트는 HTTP `status`로 오류 종류를 크게 구분하고, 화면 처리나 분기가 필요하면 `errorCode`를 사용합니다. `detail`은 사용자에게 공개 가능한 메시지이며, `requestId`는 `X-Request-Id`와 같은 값으로 서버 로그를 찾을 때 사용합니다.

- `400 Bad Request`: 요청 형식이나 입력값이 올바르지 않습니다.
- `401 Unauthorized`: 인증 정보가 없거나 access·refresh token이 유효하지 않습니다.
- `403 Forbidden`: 권한, CSRF 또는 최근 인증 조건 등을 충족하지 못해 요청이 거부되었습니다. CSRF 검사는 JWT 인증보다 먼저 실행되므로 인증이 확인되지 않은 요청에도 반환될 수 있습니다.
- `404 Not Found`: 대상 리소스가 없습니다.
- `409 Conflict`: 현재 리소스 상태나 고유값과 충돌합니다.
- `410 Gone`: 삭제되어 더 이상 사용할 수 없는 리소스입니다.
- `413 Payload Too Large`: 업로드 허용량을 초과했습니다.
- `500 Internal Server Error`: 내부 처리 또는 외부 의존성 처리에 실패했습니다.

필드별 검증 정보가 있는 경우 다음 형식의 `errors` 목록이 추가됩니다. JSON 파싱 실패나 타입 변환 오류 등에는 이 목록이 생략될 수 있습니다. `location`은 값이 없으면 생략될 수 있습니다.

```json
{
  "errors": [
    {
      "location": "body",
      "field": "email",
      "message": "이메일 형식이 올바르지 않습니다."
    }
  ]
}
```

## OAuth2 redirect 응답

OAuth2 callback 처리 결과는 JSON body로 반환하지 않고 설정된 프론트엔드 callback URL의 query parameter로 전달합니다.

OAuth 제공자의 인증을 마치고 사용자 정보를 정상적으로 확인한 경우에는 다음과 같이 redirect합니다.

```text
{frontendCallback}?auth_status={AuthStatus}
```

`auth_status`별 의미와 후속 처리는 다음과 같습니다.

- `LOGIN_SUCCESS`: 기존 사용자 로그인 성공입니다. Refresh·CSRF·최근 인증 쿠키가 발급되며, 프론트엔드는 token refresh API를 호출해 access token을 받아야 합니다.
- `SIGNUP_SUCCESS`: 신규 사용자 가입과 로그인 성공입니다. `LOGIN_SUCCESS`와 같은 쿠키가 발급되며, token refresh API로 access token을 받아야 합니다.
- `LINK_SUCCESS`: 로그인 중인 사용자에게 OAuth 계정 연결을 완료했습니다. 새로운 인증 token이나 쿠키는 발급하지 않습니다.
- `BANNED`, `SLEEP`, `DELETED`: OAuth 제공자의 인증은 성공했지만 서비스의 사용자 상태 때문에 로그인할 수 없습니다. 인증 token과 쿠키를 발급하지 않습니다.

OAuth 제공자 오류, callback 검증 실패 또는 계정 연결에 필요한 인증 실패처럼 OAuth2 처리를 완료하지 못한 경우에는 다음과 같이 redirect합니다.

```text
{frontendCallback}?error={errorCode}&error_description={message}&request_id={requestId}
```

`error`는 클라이언트 분기에 사용하는 공개 오류 코드이고 `error_description`은 사용자에게 보여 줄 수 있는 메시지입니다.

request_id는 OAuth2 실패 요청을 서버 로그와 연결하는 식별자입니다. OAuth2 성공 redirect에는 포함되지 않습니다
