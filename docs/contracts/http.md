# HTTP 공통 계약

프론트엔드와 백엔드가 함께 지키는 REST 응답·오류 계약입니다. 인증은 [인증](auth.md), 파일 업로드는 [업로드](uploads.md), 실시간 채팅은 [실시간](realtime.md)을 참고합니다.

## 기준

REST API의 공통 prefix는 `/api/v1`입니다. 상세 경로, 요청 필드와 응답 스키마는 로컬 프로필의 Swagger/OpenAPI를 기준으로 하며, 이 문서는 인터페이스 묶음과 공통 계약만 설명합니다.

## REST 영역

- `/auth`: 로컬 가입·로그인, 인증 상태, token refresh와 로그아웃
- `/user`: 공개 프로필·검색과 인증 사용자의 프로필·계정 관리
- `/adoption`: 게시글·댓글 조회와 작성, 입양자 확정
- `/chat/rooms`: 채팅방, 메시지 이력, 첨부파일과 방 나가기
- `/storage`: 업로드 URL 발급과, 로컬 저장소에서만 사용하는 서명된 파일 조회·다운로드·업로드

## 성공 응답

HTTP 성공은 `2xx` 상태로 판별하며 공통 envelope를 사용하지 않습니다. 일반 API의 응답 본문은 endpoint별 DTO, 목록, page 또는 단일 값이며 정확한 schema는 Swagger/OpenAPI를 따릅니다. `/storage`의 파일 조회·다운로드는 파일 본문을 반환합니다.

- `200 OK`: 조회·변경 결과를 응답 본문으로 반환합니다.
- `201 Created`: 리소스 생성이 완료되었습니다. endpoint에 따라 생성 결과 또는 식별자를 본문으로 반환하거나 빈 본문을 사용하며, 리소스 URI를 제공하는 경우 `Location` header가 포함됩니다.
- `204 No Content`: 작업은 성공했지만 응답 본문이 없습니다.

모든 HTTP 응답에는 서버가 생성한 `X-Request-Id` header가 포함됩니다.

현재 `X-Request-Id`는 CORS 노출 헤더에 포함되지 않아 다른 origin의 프론트엔드 JavaScript에서 직접 읽을 수 없습니다. Problem Details 오류 응답에서는 본문의 `requestId`를 사용할 수 있습니다.

## 실패 응답

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
