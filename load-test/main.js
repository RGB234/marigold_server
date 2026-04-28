import { sleep } from 'k6';
import { login } from './scenarios/auth.js';
import { getAdoptionPosts, getAdoptionPostDetail } from './scenarios/adoption.js';
import { chatSession } from './scenarios/chat.js';

export const options = {
  scenarios: {
    // 1. 단순 읽기 트래픽 (게시글 목록 및 상세 조회) - 트래픽 비중 70% 가정
    adoption_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 70 }, // Ramp-up
        { duration: '1m', target: 70 },  // Sustained 부하 유지
        { duration: '30s', target: 0 },  // Ramp-down
      ],
      exec: 'adoptionScenario',
    },
    // 2. 인증 트래픽 (로그인 시도) - 트래픽 비중 10%
    auth_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 10 },
        { duration: '30s', target: 0 },
      ],
      exec: 'authScenario',
    },
    // 3. 웹소켓 (채팅 연결) - 트래픽 비중 20%
    chat_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 },
      ],
      exec: 'chatScenario',
    },
  },
};

export function adoptionScenario() {
  const posts = getAdoptionPosts();
  sleep(1); // 실제 사용자의 페이지 응시 시간 모사

  // 무작위 게시글 하나 상세 조회
  if (posts && posts.length > 0) {
    const randomIndex = Math.floor(Math.random() * posts.length);
    getAdoptionPostDetail(posts[randomIndex].id);
  }
  sleep(1);
}

export function authScenario() {
  login();
  sleep(1);
}

export function chatScenario() {
  // 채팅 통신을 위해 로그인하여 Access Token 발급
  const { token, targetUser } = login();
  
  if (token) {
    // roomId는 실제 DB에 존재하는 채팅방 ID로 대체 가능 (현재 1로 고정하여 테스트)
    const roomId = 1; 
    // targetUser에 id가 없으면 임의의 senderId(1)를 보냄
    const senderId = targetUser.id || 1;
    
    chatSession(token, senderId, roomId);
  }
  sleep(1);
}
