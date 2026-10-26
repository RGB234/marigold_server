# 인증

REST와 WebSocket이 함께 쓰는 토큰·쿠키·CSRF 규칙과 OAuth2 redirect 계약입니다. WebSocket 단계별 검사는 [실시간](realtime.md), 서버 내부의 보안 체인 구성은 [아키텍처](../architecture.md#인증과-권한-검사)를 참고합니다.

## 토큰과 CSRF

- Access token은 응답 본문으로 발급하고 이후 `Authorization: Bearer {token}`으로 전달합니다.
- Refresh token은 HttpOnly `refresh_token` 쿠키로 발급합니다.
- REST와 WebSocket 인증은 서명, 만료, token 용도 (access or refresh) 와 claim을 검증합니다.
- CSRF token은 인증 세션에 HMAC-SHA256으로 결속한 signed double-submit token 방식이며, 읽을 수 있는 `XSRF-TOKEN` 쿠키와 `X-CSRF-TOKEN` 응답 헤더로 발급합니다.
- REST에서 `refresh_token` 또는 `recent_auth` 쿠키가 포함된 POST·PUT·PATCH·DELETE 요청은 `XSRF-TOKEN` 쿠키와 일치하는 `X-CSRF-TOKEN` 요청 헤더를 함께 보내야 합니다.
- Token refresh에 성공하면 refresh token과 CSRF token이 재발급됩니다. 이후 요청에는 새 CSRF token을 사용해야 합니다.
- 자격 증명 변경이나 회원 탈퇴 같은 민감 작업은 access token과 최근 인증을 함께 요구합니다.

## 인증 상태 복원

- `GET /auth/status`: refresh token 쿠키가 있는지 알려 줍니다. 쿠키가 있으면 CSRF token도 새로 발급합니다.
- `POST /auth/refresh`: refresh token 쿠키를 검증하고 새 access token을 본문으로, 새 refresh·CSRF token을 쿠키·헤더로 발급합니다. CSRF 헤더가 필요합니다.

페이지를 새로 열면 access token이 없으므로, 클라이언트는 `/auth/status`로 로그인 흔적을 확인한 뒤 `/auth/refresh`로 access token을 받습니다.

## 기본 유효시간

| 항목 | 기본값 |
| --- | --- |
| access token | 600초 |
| refresh token | 86,400초 (1일) |
| CSRF token | refresh token과 같음 |
| 최근 인증 | 300초 |

기준은 [application.yml](../../src/main/resources/application.yml)의 `app.auth`이며 외부 설정으로 바뀔 수 있습니다.

## 권한 변경의 반영 지연

access token을 검증할 때는 DB를 조회하지 않습니다. 사용자 상태와 권한은 refresh할 때만 DB에서 다시 읽으므로, 정지나 권한 변경은 기존 access token이 만료될 때까지(기본 최대 10분) 반영되지 않을 수 있습니다. WebSocket 연결도 같은 이유로 연결 시점의 권한을 유지하다가 토큰 만료로 끊깁니다.

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

`request_id`는 OAuth2 실패 요청을 서버 로그와 연결하는 식별자입니다. OAuth2 성공 redirect에는 포함되지 않습니다.
