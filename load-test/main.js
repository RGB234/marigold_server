import { sleep } from 'k6';
import { login } from './scenarios/auth.js';
import { getAdoptionPosts, getAdoptionPostDetail } from './scenarios/adoption.js';
import { chatSession } from './scenarios/chat.js';
import { getRandomChatFixture } from './config.js';
import { loadThresholds, summaryTrendStats } from './thresholds.js';
import { createSummary } from './summary.js';

export const options = {
  summaryTrendStats,
  thresholds: loadThresholds,
  scenarios: {
    // 1. 단순 읽기 트래픽 (게시글 목록 및 상세 조회) - 트래픽 비중 75% 가정
    adoption_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 75 }, // Ramp-up
        { duration: '1m', target: 75 },  // Sustained 부하 유지
        { duration: '30s', target: 0 },  // Ramp-down
      ],
      exec: 'adoptionScenario',
    },
    // 2. 인증 트래픽 (로그인 시도) - 트래픽 비중 5%
    auth_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 5 },
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

let cachedChatContext = null;

function getChatContext() {
  if (!cachedChatContext) {
    const { room, user } = getRandomChatFixture();
    cachedChatContext = { room, user, token: null, csrfToken: null, targetUser: user };
  }

  if (!cachedChatContext.token || !cachedChatContext.csrfToken) {
    const { token, csrfToken, targetUser } = login(cachedChatContext.user, 'chat_auth_login');
    cachedChatContext = { ...cachedChatContext, token, csrfToken, targetUser };
  }

  if (!cachedChatContext.token || !cachedChatContext.csrfToken) {
    return null;
  }

  return cachedChatContext;
}

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
  const context = getChatContext();

  if (context) {
    chatSession(context.token, context.csrfToken, context.targetUser.id, context.room.id);
  }

  sleep(1);
}

export const handleSummary = createSummary('load');
