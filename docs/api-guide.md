# API Guide

이 문서는 REST API를 사용할 때 필요한 공통 규칙만 다룹니다. 엔드포인트별 상세 요청/응답은 Swagger UI와 OpenAPI JSON을 기준으로 확인합니다.

## API 문서 소스

API 상세 명세는 컨트롤러와 DTO의 OpenAPI 애노테이션이 기준입니다.

```powershell
cd back
.\gradlew.bat bootRun --args='--spring.profiles.active=dev --springdoc.api-docs.enabled=true --springdoc.swagger-ui.enabled=true'
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

기본 설정과 운영 설정에서는 `springdoc`이 비활성화되어 있습니다.

## Base Path

REST API prefix는 `/api/v1`입니다.

| 도메인 | Base path | 설명 |
| --- | --- | --- |
| Auth | `/api/v1/auth` | 회원가입, 로그인, 인증 상태, token refresh |
| User | `/api/v1/user` | 사용자 계정, 프로필, 보안 정보 |
| Adoption | `/api/v1/adoption` | 입양 게시글, 댓글, 입양 완료 처리 |
| Chat | `/api/v1/chat` | 채팅방, 메시지, 첨부파일 URL |

## 공통 응답

모든 API 응답은 `ApiResult<T>` 형태를 사용합니다.

성공 응답 예시:

```json
{
  "success": true,
  "timestamp": "2026-06-04 12:34:56",
  "status": 200,
  "message": "fetched successfully",
  "data": {}
}
```

에러 응답 예시:

```json
{
  "success": false,
  "timestamp": "2026-06-04 12:34:56",
  "status": 400,
  "message": "입력값이 올바르지 않습니다.",
  "errorCode": "INVALID_INPUT_VALUE",
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

`data`와 `errors`는 값이 없으면 응답에서 빠질 수 있습니다.

## 인증

access token이 필요한 API는 아래 헤더를 사용합니다.

```http
Authorization: Bearer {accessToken}
```

권한 제어는 필터 체인보다 컨트롤러의 `@PreAuthorize`를 기준으로 합니다. 공개 API는 `permitAll()`, 인증 필요 API는 `isAuthenticated()`로 구분됩니다.

## CSRF

쿠키 기반 인증 상태에서 unsafe method를 호출할 때는 CSRF header를 함께 보냅니다.

대상 method:

- `POST`
- `PUT`
- `PATCH`
- `DELETE`

서버는 `XSRF-TOKEN` 쿠키와 `X-CSRF-TOKEN` 응답 헤더를 발급합니다. 프론트엔드는 unsafe 요청에 아래 헤더를 포함합니다.

```http
X-CSRF-TOKEN: {XSRF-TOKEN cookie value}
```

CSRF 검증 실패 시 `AUTH_ACCESS_DENIED` 응답을 반환합니다.

## Multipart

이미지나 파일이 포함된 요청은 `multipart/form-data`를 사용합니다. DTO와 파일 part 이름은 Swagger UI의 request schema를 기준으로 맞춥니다.

파일 검증 정책은 [검증 정책](validation-policy.md)을 기준으로 합니다.

## Pagination

목록 조회 API는 Spring `Pageable`을 사용합니다. 일반적인 query parameter는 아래와 같습니다.

```http
?page=0&size=10&sort=createdAt,desc
```

컨트롤러에서 기본 정렬을 지정한 API는 별도 query parameter가 없으면 해당 기본값을 사용합니다.

## ID 형식

외부에 노출되는 주요 ID는 TSID 문자열을 사용합니다. 검증 정책상 TSID는 Crockford Base32 13자 형식입니다.

서버 내부에서는 `Long`으로 변환해서 처리합니다.

## WebSocket

WebSocket은 [인증 흐름](auth-flow.md)의 WebSocket 섹션을 기준으로 연동합니다.
