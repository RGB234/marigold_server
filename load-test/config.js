export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const WS_BASE_URL = __ENV.WS_BASE_URL || 'ws://localhost:8080/ws/websocket';
export const API_VERSION = '/api/v1';

// 공통 테스트 데이터 및 유틸리티
export const TEST_USERS = [
  { email: 'user1@example.com', password: '!password123' },
  { email: 'user2@example.com', password: '!password123' },
  { email: 'user3@example.com', password: '!password123' },
];

export function getRandomUser() {
  return TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)];
}
