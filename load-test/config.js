export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const WS_BASE_URL = __ENV.WS_BASE_URL || 'ws://localhost:8080/ws/websocket';
export const API_VERSION = __ENV.API_VERSION || '/api/v1';
export const LOAD_TEST_PROFILE = __ENV.LOAD_TEST_PROFILE || 'smoke';
export const CHAT_SESSION_SECONDS = parsePositiveInt(__ENV.LOAD_TEST_CHAT_SESSION_SECONDS, 60);
export const IO_TEST_DURATION = __ENV.LOAD_TEST_IO_DURATION || '5m';
export const IO_PRE_ALLOCATED_VUS = parsePositiveInt(__ENV.LOAD_TEST_IO_PRE_ALLOCATED_VUS, 20);
export const IO_MAX_VUS = parsePositiveInt(__ENV.LOAD_TEST_IO_MAX_VUS, 100);
export const DB_READ_RATE = parsePositiveInt(__ENV.LOAD_TEST_DB_READ_RATE, 30);
export const DB_WRITE_RATE = parsePositiveInt(__ENV.LOAD_TEST_DB_WRITE_RATE, 5);
export const DB_MIXED_RATE = parsePositiveInt(__ENV.LOAD_TEST_DB_MIXED_RATE, 30);
export const STORAGE_UPLOAD_RATE = parsePositiveInt(__ENV.LOAD_TEST_STORAGE_UPLOAD_RATE, 2);
export const STORAGE_MIXED_RATE = parsePositiveInt(__ENV.LOAD_TEST_STORAGE_MIXED_RATE, 2);
export const STORAGE_IMAGE_COUNT = parsePositiveInt(__ENV.LOAD_TEST_STORAGE_IMAGE_COUNT, 1);
export const STORAGE_IMAGE_PATH = __ENV.LOAD_TEST_STORAGE_IMAGE_PATH || '../fixtures/storage/image-1mb.jpg';
export const STORAGE_DELETE_CREATED = parseBoolean(__ENV.LOAD_TEST_STORAGE_DELETE_CREATED, true);

const SEED_USER_COUNT = parsePositiveInt(__ENV.LOAD_TEST_SEED_USER_COUNT, 50);
const SEED_USER_ID_BASE = __ENV.LOAD_TEST_SEED_USER_ID_BASE || '990000000000000000';
const SEED_CHAT_ROOM_COUNT = parsePositiveInt(__ENV.LOAD_TEST_SEED_CHAT_ROOM_COUNT, SEED_USER_COUNT);
const SEED_CHAT_ROOM_ID_BASE = __ENV.LOAD_TEST_SEED_CHAT_ROOM_ID_BASE || '993000000000000000';
const SEED_EMAIL_PREFIX = __ENV.LOAD_TEST_SEED_EMAIL_PREFIX || 'loadtest-user-';
const SEED_EMAIL_DOMAIN = __ENV.LOAD_TEST_SEED_EMAIL_DOMAIN || 'example.test';
const LOGIN_PASSWORD = __ENV.LOAD_TEST_LOGIN_PASSWORD || '';

function parsePositiveInt(value, fallback) {
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function parseBoolean(value, fallback) {
  if (value === undefined || value === null || value === '') {
    return fallback;
  }
  return String(value).toLowerCase() === 'true';
}

export const ADOPTION_PAGE_SIZE = parsePositiveInt(__ENV.LOAD_TEST_ADOPTION_PAGE_SIZE, 10);
export const ADOPTION_TOTAL_PAGES = parsePositiveInt(__ENV.LOAD_TEST_ADOPTION_TOTAL_PAGES, 1);
export const ADOPTION_FIXED_PAGE = Math.max(
  0,
  parsePositiveInt(__ENV.LOAD_TEST_ADOPTION_FIXED_PAGE, 0)
);
export const ADOPTION_PAGE_SELECTION =
  __ENV.LOAD_TEST_ADOPTION_PAGE_SELECTION || (LOAD_TEST_PROFILE === 'smoke' ? 'fixed' : 'weighted');

function randomPage(min, max) {
  if (max <= min) {
    return min;
  }
  return min + Math.floor(Math.random() * (max - min + 1));
}

export function selectAdoptionPage() {
  const lastPage = Math.max(0, ADOPTION_TOTAL_PAGES - 1);

  if (ADOPTION_PAGE_SELECTION === 'fixed') {
    return Math.min(ADOPTION_FIXED_PAGE, lastPage);
  }

  if (ADOPTION_PAGE_SELECTION === 'random') {
    return randomPage(0, lastPage);
  }

  const roll = Math.random();
  if (roll < 0.7) {
    return randomPage(0, Math.min(2, lastPage));
  }
  if (roll < 0.9) {
    return randomPage(Math.min(3, lastPage), Math.min(20, lastPage));
  }
  return randomPage(Math.min(21, lastPage), lastPage);
}

function leftPadNumber(value, width) {
  const text = String(value);
  if (text.length >= width) {
    return text;
  }
  return '0'.repeat(width - text.length) + text;
}

function addPositiveIntToDecimalString(value, addend) {
  let carry = addend;
  let result = '';

  for (let i = value.length - 1; i >= 0; i--) {
    const digit = value.charCodeAt(i) - 48;
    const sum = digit + (carry % 10);
    result = String(sum % 10) + result;
    carry = Math.floor(carry / 10) + Math.floor(sum / 10);
  }

  while (carry > 0) {
    result = String(carry % 10) + result;
    carry = Math.floor(carry / 10);
  }

  return result.replace(/^0+(?=\d)/, '');
}

function createSeedUser(userNumber) {
  if (!LOGIN_PASSWORD) {
    throw new Error("LOAD_TEST_LOGIN_PASSWORD is required for seeded user login.");
  }

  return {
    id: addPositiveIntToDecimalString(SEED_USER_ID_BASE, userNumber),
    number: userNumber,
    email: `${SEED_EMAIL_PREFIX}${leftPadNumber(userNumber, 6)}@${SEED_EMAIL_DOMAIN}`,
    password: LOGIN_PASSWORD,
  };
}

export function getRandomUser() {
  const userNumber = 1 + Math.floor(Math.random() * SEED_USER_COUNT);
  return createSeedUser(userNumber);
}

export function getRandomChatRoom() {
  const roomNumber = 1 + Math.floor(Math.random() * SEED_CHAT_ROOM_COUNT);
  return {
    id: addPositiveIntToDecimalString(SEED_CHAT_ROOM_ID_BASE, roomNumber),
    number: roomNumber,
  };
}

export function getRandomChatFixture() {
  const room = getRandomChatRoom();
  const userNumber = 1 + ((room.number - 1) % SEED_USER_COUNT);
  return { room, user: createSeedUser(userNumber) };
}
