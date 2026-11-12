# 인증

REST와 WebSocket이 함께 쓰는 토큰·쿠키·CSRF 규칙과 OAuth2 redirect 계약입니다. WebSocket 단계별 검사는 [실시간](realtime.md), 서버 내부의 보안 체인 구성은 [아키텍처](../architecture.md#인증과-권한-검사)를 참고합니다.

## 토큰과 CSRF

- 로그인할 때마다 서버는 인증 세션을 하나 만들고, access·refresh token 모두 그 세션에 묶입니다. 세션이 폐기되면 두 token 모두 만료 전이라도 거부됩니다.
- Access token은 응답 본문으로 발급하고 이후 `Authorization: Bearer {token}`으로 전달합니다.
- Refresh token은 HttpOnly `refresh_token` 쿠키로 발급하는 불투명 값입니다. JWT가 아니고 claim도 없으므로 클라이언트는 값을 해석하지 않습니다. 서버는 값의 해시만 보관하며, 유효한지와 만료는 서버의 세션이 정합니다.
- REST와 WebSocket 인증은 access token의 서명, 만료, 용도, claim과 인증 세션이 살아 있는지를 검증합니다.
- 프론트엔드와 백엔드는 같은 사이트(같은 등록 도메인과 scheme, 예: `https://app.example.com`과 `https://api.example.com`)에 있어야 합니다. 서버가 발급하는 쿠키는 모두 HttpOnly, Secure, `SameSite=Lax`입니다.
- CSRF: 쿠키만으로 인증되는 요청은 `POST /auth/refresh`와 `POST /auth/logout`뿐이고, `SameSite=Lax` 쿠키는 다른 사이트에서 보낸 POST에 실리지 않으므로 별도 CSRF token이나 헤더를 쓰지 않습니다. 같은 사이트의 다른 서브도메인은 이 방어 밖이므로 신뢰할 수 없는 내용을 올리지 않아야 합니다.
- `Strict`가 아니라 `Lax`인 이유: OAuth2 callback은 제공자 사이트에서 넘어오는 GET 이동이라 state·refresh 쿠키가 실려야 합니다.
- 그 밖의 API와 STOMP CONNECT는 `Authorization` 헤더로 인증하므로 CSRF 대상이 아닙니다.
- Token refresh에 성공하면 refresh token이 재발급됩니다.
- Refresh token은 한 번만 쓸 수 있습니다. 직전 refresh token은 회전 뒤 짧은 유예 시간 동안만 다시 받아 줍니다(응답을 받지 못한 재시도용). 유예가 지난 직전 token이나 그보다 오래된 token이 오면 탈취로 보고 세션을 폐기하며 `401 AUTH_TOKEN_INVALID`를 반환합니다. 같은 refresh 쿠키를 쓰는 탭끼리는 refresh를 동시에 보내지 않아야 합니다.
- 자격 증명 등록, 회원 탈퇴, OAuth 계정 연결은 최근 인증을 요구합니다. 요청의 인증 세션이 로그인한 지 최근 인증 유효시간 안이어야 하며, 아니면 `403 AUTH_RECENT_AUTH_REQUIRED`입니다. refresh로는 갱신되지 않으므로 다시 로그인해야 합니다. 최근 인증용 쿠키는 따로 없습니다.

## 인증 상태 복원

- `GET /auth/status`: refresh token 쿠키가 있는지 알려 줍니다.
- `POST /auth/refresh`: refresh token 쿠키를 검증하고 새 access token을 본문으로, 새 refresh token을 쿠키로 발급합니다.
- `POST /auth/logout`: refresh 쿠키를 지우고 세션을 폐기합니다. 이후 그 세션의 access token도 `401 AUTH_TOKEN_INVALID`로 거부됩니다. 세션 저장소가 응답하지 않아도 쿠키는 지우고 `204`로 끝납니다.

페이지를 새로 열면 access token이 없으므로, 클라이언트는 `/auth/status`로 로그인 흔적을 확인한 뒤 `/auth/refresh`로 access token을 받습니다.

## 기본 유효시간

| 항목 | 기본값 |
| --- | --- |
| access token | 600초 |
| refresh token | 86,400초 (1일). 세션 절대 만료를 넘지 않음 |
| 세션 절대 만료 | 1,209,600초 (2주). refresh로 연장해도 로그인 후 이 시간이 지나면 다시 로그인해야 함 |
| 직전 refresh token 재시도 유예 | 10초 |
| 최근 인증 | 300초 (로그인 시각 기준) |

기준은 [application.yml](../../src/main/resources/application.yml)의 `app.auth`이며 외부 설정으로 바뀔 수 있습니다.

## 세션 폐기와 권한 변경의 반영

access token을 검증할 때는 DB를 조회하지 않고, 인증 세션이 살아 있는지만 확인합니다.

- 로그아웃, refresh token 재사용 감지, 회원 탈퇴는 세션을 폐기하므로 다음 요청부터 바로 반영됩니다. 탈퇴는 그 사용자의 모든 세션을 폐기합니다.
- refresh 쿠키가 실린 채로 다시 로그인하면(로컬 로그인, OAuth2 로그인·가입) 새 세션을 만든 뒤 그 쿠키의 세션을 폐기합니다. 다른 기기나 브라우저의 세션은 남습니다. OAuth 계정 연결은 세션을 새로 만들지 않으므로 해당하지 않습니다. 이전 세션 폐기에 실패해도 로그인은 성공하며, 그 세션은 만료될 때 사라집니다.
- 정지는 관리자 API로 합니다. `POST /user/{userId}/ban`은 사용자를 정지하고 그 사용자의 모든 세션을 폐기하므로 다음 요청부터 바로 반영됩니다. `DELETE /user/{userId}/ban`은 정지를 해제합니다. 둘 다 `ROLE_ADMIN`만 호출할 수 있고 성공하면 `204`입니다.
  - 대상이 관리자이면 `403 USER_ADMIN_CANNOT_BE_BANNED`, 탈퇴한 사용자이면 `403 USER_DELETED`입니다.
  - 정지는 저장됐지만 세션 저장소 장애로 세션을 폐기하지 못하면 `503 AUTH_SESSION_STORE_UNAVAILABLE`입니다. 이미 정지된 사용자에게 다시 보내도 세션을 폐기하므로 같은 요청을 다시 보냅니다.
  - 해제는 정지 상태가 아닌 사용자를 바꾸지 않고 `204`를 돌려줍니다.
- 사용자 상태와 권한은 refresh할 때만 DB에서 다시 읽습니다. 권한 변경과, 관리자 API를 거치지 않고 DB에서 직접 바꾼 상태는 기존 access token이 만료될 때까지(기본 최대 10분) 반영되지 않을 수 있습니다. 관리자 권한을 잃은 사용자도 그동안은 관리자 API를 호출할 수 있습니다.
- WebSocket 연결은 연결 시점의 권한을 유지하다가 토큰 만료로 끊기고, 세션이 폐기되면 그 전에 끊깁니다([실시간](realtime.md#연결-종료와-재연결)).

## 세션 저장소 장애

세션 저장소가 응답하지 않으면 인증이 필요한 요청, 로그인과 refresh는 `503 AUTH_SESSION_STORE_UNAVAILABLE`로 실패합니다. token이 잘못된 것이 아니므로 `401`을 쓰지 않으며, 클라이언트는 로그인 상태를 지우지 말고 잠시 뒤 다시 시도합니다. OAuth2 로그인 중에 실패하면 같은 코드로 오류 redirect합니다.

## OAuth2 redirect 응답

OAuth2 callback 처리 결과는 JSON body로 반환하지 않고 설정된 프론트엔드 callback URL의 query parameter로 전달합니다.

OAuth 제공자의 인증을 마치고 사용자 정보를 정상적으로 확인한 경우에는 다음과 같이 redirect합니다.

```text
{frontendCallback}?auth_status={AuthStatus}
```

`auth_status`별 의미와 후속 처리는 다음과 같습니다.

- `LOGIN_SUCCESS`: 기존 사용자 로그인 성공입니다. Refresh 쿠키가 발급되며, 프론트엔드는 token refresh API를 호출해 access token을 받아야 합니다.
- `SIGNUP_SUCCESS`: 신규 사용자 가입과 로그인 성공입니다. `LOGIN_SUCCESS`와 같은 쿠키가 발급되며, token refresh API로 access token을 받아야 합니다.
- `LINK_SUCCESS`: 로그인 중인 사용자에게 OAuth 계정 연결을 완료했습니다. 새로운 인증 token이나 쿠키는 발급하지 않습니다.
- `BANNED`, `DELETED`: OAuth 제공자의 인증은 성공했지만 서비스의 사용자 상태 때문에 로그인할 수 없습니다. 인증 token과 쿠키를 발급하지 않습니다.

OAuth 제공자 오류, callback 검증 실패 또는 계정 연결에 필요한 인증 실패처럼 OAuth2 처리를 완료하지 못한 경우에는 다음과 같이 redirect합니다.

```text
{frontendCallback}?error={errorCode}&error_description={message}&request_id={requestId}
```

`error`는 클라이언트 분기에 사용하는 공개 오류 코드이고 `error_description`은 사용자에게 보여 줄 수 있는 메시지입니다.

`request_id`는 OAuth2 실패 요청을 서버 로그와 연결하는 식별자입니다. OAuth2 성공 redirect에는 포함되지 않습니다.
