import ws from 'k6/ws';
import { check } from 'k6';
import { WS_BASE_URL } from '../config.js';

function createStompFrame(command, headers = {}, body = '') {
  let frame = `${command}\n`;
  for (const [key, value] of Object.entries(headers)) {
    frame += `${key}:${value}\n`;
  }
  frame += `\n${body}\0`;
  return frame;
}

export function chatSession(token, userId, roomId) {
  if (!token || !userId || !roomId) {
    // If not authenticated or missing room, skip chat test
    return;
  }

  const url = WS_BASE_URL;
  const params = {
    tags: { my_tag: 'websocket' }
  };

  const res = ws.connect(url, params, function (socket) {
    socket.on('open', () => {
      // 1. CONNECT 전송
      const connectFrame = createStompFrame('CONNECT', {
        'accept-version': '1.1,1.2',
        'heart-beat': '10000,10000',
        'Authorization': `Bearer ${token}`
      });
      socket.send(connectFrame);
    });

    socket.on('message', (msg) => {
      const isConnected = msg.startsWith('CONNECTED');
      if (isConnected) {
        // 연결 성공
        check(msg, { 'websocket CONNECTED': (m) => m.startsWith('CONNECTED') });

        // 2. SUBSCRIBE (방 입장)
        const subscribeFrame = createStompFrame('SUBSCRIBE', {
          id: 'sub-0',
          destination: `/sub/chat/room/${roomId}`
        });
        socket.send(subscribeFrame);

        // 3. SEND (메시지 전송) - 구독 직후 발송
        // 실제로는 구독 확인 후 발송하는 것이 좋지만, 부하 테스트를 위해 연속 전송
        const messagePayload = JSON.stringify({
          roomId: roomId,
          senderId: userId,
          message: 'Hello from k6 load test',
        });

        const sendFrame = createStompFrame('SEND', {
          destination: '/pub/chat/message',
          'content-type': 'application/json'
        }, messagePayload);

        socket.send(sendFrame);

        // 4. 세션 종료를 위한 타이머 (5초 대기 후 종료)
        socket.setTimeout(function () {
          socket.close();
        }, 5000);
      }

      // 메시지 수신 (MESSAGE 프레임)
      if (msg.startsWith('MESSAGE')) {
        check(msg, { 'websocket received MESSAGE': (m) => m.includes('Hello from k6 load test') });
      }
    });

    socket.on('close', () => {
      // 연결 종료
    });

    socket.on('error', (e) => {
      if (e.error() != 'websocket: close sent') {
        console.error('An unexpected error occurred: ', e.error());
      }
    });
  });

  check(res, { 'websocket session successfully finished': (r) => r && r.status === 101 });
}
