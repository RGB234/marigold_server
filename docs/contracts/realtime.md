# 실시간 채팅 (WebSocket/STOMP)

채팅방·메시지 이력·첨부파일 메시지는 REST로, 텍스트 메시지 송수신은 STOMP로 처리합니다. 토큰과 CSRF 자체의 규칙은 [인증](auth.md)을 참고합니다.

채팅은 SockJS endpoint `/ws`를 사용합니다.

- CONNECT native header: `Authorization: Bearer {token}`, `X-CSRF-TOKEN: {token}`
- SEND: `/pub/chat/message`
- SUBSCRIBE: `/sub/chat/room/{roomId}`
- 사용자 오류 queue: `/user/queue/errors`

구독 경로의 `{roomId}`는 13자리 TSID 문자열입니다.

## 단계별 검사

| 단계 | 서버가 확인하는 것 |
| --- | --- |
| 핸드셰이크 | 허용된 프론트엔드 origin인지. `XSRF-TOKEN` 쿠키와 `refresh_token`(없으면 `recent_auth`) 쿠키 값을 CSRF 결속 재료로 세션에 보관 |
| CONNECT | `X-CSRF-TOKEN`이 쿠키와 같고 보관한 결속 값으로 서명되었는지, access token의 서명·만료·용도. 토큰 만료 시각을 세션에 기록 |
| SUBSCRIBE | CONNECT를 통과한 세션인지, 기록한 만료 시각이 지나지 않았는지, 허용된 경로인지, 방 구독이면 DB에서 나가지 않은 참여자인지 |
| SEND | CONNECT를 통과한 세션인지, 경로가 `/pub/chat/message`인지, 기록한 만료 시각이 지나지 않았는지. 저장할 때 참여 여부와 방 종료 여부를 DB에서 다시 확인 |

refresh token은 핸드셰이크에서 CSRF 결속 값으로만 쓰이며 토큰으로 검증하지 않습니다. CONNECT 이후에는 JWT를 다시 검증하지 않습니다.

## 성공 응답

- CONNECT가 성공하면 STOMP `CONNECTED` frame을 받습니다.
- SUBSCRIBE 성공에는 별도 application payload가 없습니다. 이후 `/sub/chat/room/{roomId}`로 수신되는 메시지는 `ChatMessageDto` 형식입니다.
- `/pub/chat/message`로 보낸 메시지가 저장되면 같은 방의 `/sub/chat/room/{roomId}`로 `ChatMessageDto`를 발행합니다. SEND 요청 자체에 대한 별도 application ACK는 없습니다.
- HTTP로 생성한 첨부파일 메시지도 HTTP `201 Created` 응답과 별개로 같은 방 구독 경로에 발행됩니다.

메시지 응답(`ChatMessageDto`)에는 TSID 문자열 `id`가 있습니다.

방을 나간 사용자는 메시지 이력과 첨부파일 REST API도 사용할 수 없습니다. 다시 참여하면 나가기 전 대화를 포함한 전체 이력을 볼 수 있습니다.
같은 방의 다른 참여자가 메시지(텍스트 또는 첨부파일)를 저장하면 나간 사용자는 자동으로 다시 참여하고, 방은 그 사용자의 채팅방 목록에 다시 나타납니다.
다시 참여한 뒤에는 그 방의 SUBSCRIBE가 다시 허용됩니다.

## 실패 응답

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

## 연결 종료와 재연결

| close code | reason | 언제 |
| --- | --- | --- |
| `4001` | `JWT expired` | CONNECT에 쓴 access token이 만료될 때 |
| `4003` | `Left chat room` | `DELETE /chat/rooms/{roomId}/leave`로 방을 나갈 때, 그 사용자가 그 방을 구독 중인 연결 |

연결 중에는 토큰을 연장할 수 없으므로, 연결은 access token 유효시간(기본 600초)마다 끊깁니다. 클라이언트는 새 access token을 발급받아 다시 연결합니다. `4003` 뒤에 다시 연결해도 다시 참여하기 전까지 그 방의 SUBSCRIBE는 거부됩니다.

연결이 끊긴 동안의 메시지는 실시간으로 다시 오지 않습니다. 클라이언트는 연결될 때마다 REST 이력을 읽어 `id`로 중복을 걸러 합칩니다.

`4003` 종료는 요청을 받은 서버의 연결에만 적용됩니다. 다중 서버 제약은 [아키텍처](../architecture.md#재시작과-다중-서버)를 참고합니다.
