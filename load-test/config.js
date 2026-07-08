import { SharedArray } from 'k6/data';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const WS_BASE_URL = __ENV.WS_BASE_URL || 'ws://localhost:8080/ws/websocket';
export const API_VERSION = '/api/v1';

const DEFAULT_TEST_PASSWORD = __ENV.LOAD_TEST_USER_PASSWORD || '';
const USERS_FILE = __ENV.LOAD_TEST_USERS_FILE || './data/users.csv';
const ROOMS_FILE = __ENV.LOAD_TEST_ROOMS_FILE || './data/rooms.json';

function parseUsersCsv(text) {
  return text
    .split('\n')
    .slice(1)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [id, email, password] = line.split(',').map((value) => value.trim());
      return { id: Number(id), email, password: password || DEFAULT_TEST_PASSWORD };
    })
    .filter((user) => user.email && user.password);
}

function fallbackUsers() {
  return [
    {
      id: Number(__ENV.LOAD_TEST_USER1_ID || 1),
      email: __ENV.LOAD_TEST_USER1_EMAIL || 'user1@example.com',
      password: __ENV.LOAD_TEST_USER1_PASSWORD || DEFAULT_TEST_PASSWORD,
    },
    {
      id: Number(__ENV.LOAD_TEST_USER2_ID || 2),
      email: __ENV.LOAD_TEST_USER2_EMAIL || 'user2@example.com',
      password: __ENV.LOAD_TEST_USER2_PASSWORD || DEFAULT_TEST_PASSWORD,
    },
    {
      id: Number(__ENV.LOAD_TEST_USER3_ID || 3),
      email: __ENV.LOAD_TEST_USER3_EMAIL || 'user3@example.com',
      password: __ENV.LOAD_TEST_USER3_PASSWORD || DEFAULT_TEST_PASSWORD,
    },
  ].filter((user) => user.email && user.password);
}

export const TEST_USERS = new SharedArray('test users', () => {
  const fileUsers = parseUsersCsv(open(USERS_FILE));
  return fileUsers.length > 0 ? fileUsers : fallbackUsers();
});

export const CHAT_ROOMS = new SharedArray('chat rooms', () => {
  const rooms = JSON.parse(open(ROOMS_FILE));
  return Array.isArray(rooms) ? rooms.filter((room) => room.id) : [];
});

export function getRandomUser() {
  return TEST_USERS[Math.floor(Math.random() * TEST_USERS.length)];
}

export function getRandomChatRoom() {
  if (CHAT_ROOMS.length === 0) {
    return { id: Number(__ENV.LOAD_TEST_ROOM_ID || 1) };
  }
  return CHAT_ROOMS[Math.floor(Math.random() * CHAT_ROOMS.length)];
}
