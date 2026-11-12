import { sleep } from 'k6';
import { login } from './auth.js';
import { getAdoptionPosts, getAdoptionPostDetail } from './adoption.js';
import { chatSession } from './chat.js';
import { getRandomChatFixture } from '../config.js';

let cachedChatContext = null;

function getChatContext() {
  if (!cachedChatContext) {
    const { room, user } = getRandomChatFixture();
    cachedChatContext = { room, user, token: null, targetUser: user };
  }

  if (!cachedChatContext.token) {
    const { token, targetUser } = login(cachedChatContext.user, 'chat_auth_login');
    cachedChatContext = { ...cachedChatContext, token, targetUser };
  }

  if (!cachedChatContext.token) {
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
    chatSession(context.token, context.targetUser.id, context.room.id);
  }

  sleep(1);
}
