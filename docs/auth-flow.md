# Auth Flow

이 문서는 프론트엔드 연동에 필요한 인증 흐름을 정리합니다.

## 토큰과 쿠키

| 이름 | 전달 위치 | 설명 |
| --- | --- | --- |
| Access token | API 응답 body | REST API와 STOMP 인증에 사용하는 bearer token |
| `refresh_token` | HttpOnly Secure cookie | access token 재발급에 사용하는 refresh token |
| `XSRF-TOKEN` | readable Secure cookie | CSRF double-submit 검증용 token |
| `X-CSRF-TOKEN` | response/request header | 서버가 발급하고 클라이언트가 unsafe 요청에 다시 보내는 CSRF header |
| `recent_auth` | HttpOnly Secure cookie | 민감 작업의 최근 인증 상태 |

쿠키는 `Secure`, `SameSite=None`, path `/`로 발급됩니다. 로컬 HTTP 환경에서는 브라우저가 Secure cookie를 저장하지 않을 수 있으므로, 브라우저 연동 테스트는 HTTPS 환경 차이를 확인해야 합니다.

## 로컬 회원가입

```http
POST /api/v1/auth/signup
Content-Type: application/json
```

성공 시 `201`을 반환합니다. 입력 검증 정책은 [검증 정책](validation-policy.md)을 따릅니다.

## 로컬 로그인

```http
POST /api/v1/auth/login
Content-Type: application/json
```

성공 시 응답 body에 access token을 반환하고, refresh token과 CSRF token을 쿠키로 발급합니다. CSRF token은 `X-CSRF-TOKEN` 응답 헤더로도 노출됩니다.

프론트엔드는 access token을 API 요청의 `Authorization` 헤더에 사용합니다.

```http
Authorization: Bearer {accessToken}
```

## 인증 상태 조회

```http
GET /api/v1/auth/status
```

현재 access token 인증 상태와 refresh token 쿠키 존재 여부를 조회합니다. refresh token 쿠키가 있으면 서버가 새 CSRF token을 발급합니다.

## Access Token 재발급

```http
POST /api/v1/auth/refresh
X-CSRF-TOKEN: {XSRF-TOKEN cookie value}
```

refresh token 쿠키로 새 access token과 refresh token을 발급합니다. 쿠키 기반 인증 상태에서 호출하는 unsafe method이므로 CSRF header가 필요합니다.

## Logout

```http
POST /api/v1/auth/logout
X-CSRF-TOKEN: {XSRF-TOKEN cookie value}
```

Spring Security logout handler가 refresh token, CSRF token 등 인증 쿠키를 만료합니다.

## OAuth2 로그인

OAuth2 경로는 `/oauth2/**` security chain에서 처리합니다.

시작 URL:

```http
GET /oauth2/authorization/kakao
GET /oauth2/authorization/naver
```

provider callback:

```http
GET /oauth2/code/kakao
GET /oauth2/code/naver
```

OAuth2 성공 시 서버는 정상 상태에서 refresh token, CSRF token, recent auth token을 발급한 뒤 프론트 callback URL로 redirect합니다.

```http
{FRONTEND_ORIGIN}/auth/callback?auth_status={AUTH_STATUS}
```

실패 시 callback URL에 에러 정보를 붙여 redirect합니다.

```http
{FRONTEND_ORIGIN}/auth/callback?error={ERROR_CODE}&error_description={MESSAGE}
```

## CSRF

쿠키 기반 인증 상태에서 `POST`, `PUT`, `PATCH`, `DELETE` 요청을 보낼 때는 서버가 발급한 `XSRF-TOKEN` 쿠키 값을 `X-CSRF-TOKEN` 헤더로 함께 전송합니다.

## CORS

허용 origin은 `FRONTEND_ORIGIN`에서 scheme, host, port를 추출한 값입니다. credential 요청을 허용하고, `X-CSRF-TOKEN` 응답 헤더를 노출합니다.

## WebSocket 인증

WebSocket endpoint는 `/ws`이며 SockJS를 사용합니다.

STOMP prefix:

- publish: `/pub`
- subscribe: `/sub`

텍스트 메시지 publish destination:

```text
/pub/chat/message
```

채팅방 subscribe destination:

```text
/sub/chat/room/{roomId}
```

STOMP `CONNECT` 요청에는 아래 native header를 포함합니다. 이후 같은 연결에서 메시지 전송과 구독을 수행합니다.

```http
Authorization: Bearer {accessToken}
X-CSRF-TOKEN: {XSRF-TOKEN cookie value}
```

채팅방 구독은 참여자만 허용됩니다. `/sub/chat/room/{roomId}`의 `roomId`는 TSID 문자열 또는 Long 문자열로 해석됩니다.
