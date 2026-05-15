export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const WS_BASE_URL = __ENV.WS_BASE_URL || 'ws://localhost:8080/ws/websocket';
export const API_VERSION = '/api/v1';

// 공통 테스트 데이터 및 유틸리티
const DEFAULT_TEST_PASSWORD = __ENV.LOAD_TEST_USER_PASSWORD || '';

export const TEST_USERS = [
  {
    email: __ENV.LOAD_TEST_USER1_EMAIL || 'user1@example.com',
    password: __ENV.LOAD_TEST_USER1_PASSWORD || DEFAULT_TEST_PASSWORD,
  },
  {
    email: __ENV.LOAD_TEST_USER2_EMAIL || 'user2@example.com',
    password: __ENV.LOAD_TEST_USER2_PASSWORD || DEFAULT_TEST_PASSWORD,
  },
  {
    email: __ENV.LOAD_TEST_USER3_EMAIL || 'user3@example.com',
    password: __ENV.LOAD_TEST_USER3_PASSWORD || DEFAULT_TEST_PASSWORD,
  },
];

export function getRandomUser() {
  return TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)];
}
