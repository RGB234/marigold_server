import http from 'k6/http';
import { check } from 'k6';
import {
  API_VERSION,
  BASE_URL,
  STORAGE_DELETE_CREATED,
  STORAGE_IMAGE_COUNT,
  STORAGE_IMAGE_PATH,
} from '../config.js';
import { authHeaders, getAuthContext } from './auth-context.js';

const TEST_IMAGE_BYTES = open(STORAGE_IMAGE_PATH, 'b');

// 파일 PUT은 서명된 URL로만 인증한다. 우리 서버 쿠키가 붙으면 CSRF 검사 대상이 되거나 S3 서명이 깨지므로 빈 jar를 쓴다.
let uploadJar = null;

function shortTitle(prefix) {
  return `${prefix}${__VU}-${__ITER}`.slice(0, 16);
}

function adoptionPostBody(titlePrefix, uploadTokens) {
  return {
    species: 'DOG',
    title: shortTitle(titlePrefix),
    age: 2,
    sex: 'MALE',
    area: 'Seoul',
    weight: 5.0,
    neutering: 'YES',
    features: 'Load test adoption feature text.',
    uploadTokens,
  };
}

function jsonHeaders(context) {
  return authHeaders(context, { 'Content-Type': 'application/json' });
}

// 업로드 URL 발급 → 파일별 PUT 순서로 올리고 도메인 API에 보낼 업로드 토큰을 돌려준다. 실패하면 null.
function uploadImages(context, metricPrefix, imageCount = STORAGE_IMAGE_COUNT) {
  if (!context) {
    return null;
  }

  const count = Math.max(1, imageCount);
  const files = [];
  for (let i = 1; i <= count; i++) {
    files.push({ fileName: `load-test-${i}.jpg`, size: TEST_IMAGE_BYTES.byteLength });
  }

  const issueRes = http.post(
    `${BASE_URL}${API_VERSION}/storage/uploads`,
    JSON.stringify({ purpose: 'ADOPTION_POST_IMAGE', files }),
    {
      headers: jsonHeaders(context),
      tags: { name: `${metricPrefix}_upload_issue` },
    }
  );

  const isIssued = check(issueRes, {
    [`${metricPrefix}_upload_issue status is 200`]: (r) => r.status === 200,
    [`${metricPrefix}_upload_issue returns every file`]: (r) =>
      r.status === 200 && Boolean(r.body) && Array.isArray(r.json()) && r.json().length === count,
  });
  if (!isIssued) {
    return null;
  }

  uploadJar = uploadJar || new http.CookieJar();
  const uploadTokens = [];
  for (const upload of issueRes.json()) {
    const putRes = http.put(upload.uploadUrl, TEST_IMAGE_BYTES, {
      headers: { 'Content-Type': upload.contentType },
      jar: uploadJar,
      tags: { name: `${metricPrefix}_upload_put` },
    });

    const isUploaded = check(putRes, {
      [`${metricPrefix}_upload_put status is 200`]: (r) => r.status === 200,
    });
    if (!isUploaded) {
      return null;
    }
    uploadTokens.push(upload.uploadToken);
  }

  return uploadTokens;
}

export function createAdoptionPostWithImages(
  context,
  metricName = 'storage_post_create',
  metricPrefix = 'storage',
  imageCount = STORAGE_IMAGE_COUNT
) {
  const uploadTokens = uploadImages(context, metricPrefix, imageCount);
  if (!uploadTokens) {
    return null;
  }

  const res = http.post(
    `${BASE_URL}${API_VERSION}/adoption`,
    JSON.stringify(adoptionPostBody('LT', uploadTokens)),
    {
      headers: jsonHeaders(context),
      tags: { name: metricName },
    }
  );

  const isSuccessful = check(res, {
    [`${metricName} status is 201`]: (r) => r.status === 201,
    [`${metricName} created id exists`]: (r) => r.status === 201 && Boolean(r.body) && Boolean(r.json('id')),
  });

  return isSuccessful ? res.json('id') : null;
}

export function updateAdoptionPostWithImages(
  context,
  postId,
  metricName = 'storage_post_update',
  metricPrefix = 'storage',
  imageCount = STORAGE_IMAGE_COUNT
) {
  if (!context || !postId) {
    return false;
  }

  const uploadTokens = uploadImages(context, metricPrefix, imageCount);
  if (!uploadTokens) {
    return false;
  }

  // 기존 이미지는 모두 새 이미지로 교체한다.
  const res = http.patch(
    `${BASE_URL}${API_VERSION}/adoption/${postId}`,
    JSON.stringify({ ...adoptionPostBody('LU', uploadTokens), imagesToKeep: [] }),
    {
      headers: jsonHeaders(context),
      tags: { name: metricName },
    }
  );

  return check(res, {
    [`${metricName} status is 204`]: (r) => r.status === 204,
  });
}

export function deleteAdoptionPost(context, postId, metricName = 'storage_post_delete') {
  if (!context || !postId) {
    return false;
  }

  const res = http.del(`${BASE_URL}${API_VERSION}/adoption/${postId}`, null, {
    headers: authHeaders(context),
    tags: { name: metricName },
  });

  return check(res, {
    [`${metricName} status is 204`]: (r) => r.status === 204,
  });
}

export function storageUploadScenario() {
  const context = getAuthContext('storage_auth_login');
  const postId = createAdoptionPostWithImages(context, 'storage_post_create', 'storage');

  if (STORAGE_DELETE_CREATED) {
    deleteAdoptionPost(context, postId, 'storage_post_delete');
  }
}

export function storageMixedScenario() {
  const context = getAuthContext('storage_mixed_auth_login');
  const postId = createAdoptionPostWithImages(context, 'storage_mixed_post_create', 'storage_mixed');

  if (!postId) {
    return;
  }

  updateAdoptionPostWithImages(context, postId, 'storage_mixed_post_update', 'storage_mixed');
  deleteAdoptionPost(context, postId, 'storage_mixed_post_delete');
}
