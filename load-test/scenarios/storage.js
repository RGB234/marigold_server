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

function testImage(index) {
  return http.file(TEST_IMAGE_BYTES, `load-test-${index}.jpg`, 'image/jpeg');
}

function imageField(imageCount) {
  const count = Math.max(1, imageCount);
  if (count === 1) {
    return testImage(1);
  }

  const images = [];
  for (let i = 1; i <= count; i++) {
    images.push(testImage(i));
  }
  return images;
}

function shortTitle(prefix) {
  return `${prefix}${__VU}-${__ITER}`.slice(0, 16);
}

function adoptionPostBody(titlePrefix, imageCount) {
  return {
    species: 'DOG',
    title: shortTitle(titlePrefix),
    age: '2',
    sex: 'MALE',
    area: 'Seoul',
    weight: '5.0',
    neutering: 'YES',
    features: 'Load test adoption feature text.',
    images: imageField(imageCount),
  };
}

function extractCreatedId(response) {
  if (!response || !response.body) {
    return null;
  }
  const match = response.body.match(/"id"\s*:\s*"?([^",}]+)"?/);
  return match ? match[1] : null;
}

export function createAdoptionPostWithImages(
  context,
  metricName = 'storage_post_create',
  imageCount = STORAGE_IMAGE_COUNT
) {
  if (!context) {
    return null;
  }

  const res = http.post(`${BASE_URL}${API_VERSION}/adoption`, adoptionPostBody('LT', imageCount), {
    headers: authHeaders(context),
    tags: { name: metricName },
  });

  const isSuccessful = check(res, {
    [`${metricName} status is 201`]: (r) => r.status === 201,
    [`${metricName} success is true`]: (r) => r.status === 201 && Boolean(r.body) && r.json('success') === true,
  });
  const postId = isSuccessful ? extractCreatedId(res) : null;

  const hasCreatedId = check(res, {
    [`${metricName} created id exists`]: () => !isSuccessful || Boolean(postId),
  });

  return isSuccessful && hasCreatedId ? postId : null;
}

export function updateAdoptionPostWithImages(
  context,
  postId,
  metricName = 'storage_post_update',
  imageCount = STORAGE_IMAGE_COUNT
) {
  if (!context || !postId) {
    return false;
  }

  const res = http.patch(
    `${BASE_URL}${API_VERSION}/adoption/${postId}`,
    adoptionPostBody('LU', imageCount),
    {
      headers: authHeaders(context),
      tags: { name: metricName },
    }
  );

  return check(res, {
    [`${metricName} status is 200`]: (r) => r.status === 200,
    [`${metricName} success is true`]: (r) => r.status === 200 && Boolean(r.body) && r.json('success') === true,
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
    [`${metricName} status is 200`]: (r) => r.status === 200,
    [`${metricName} success is true`]: (r) => r.status === 200 && Boolean(r.body) && r.json('success') === true,
  });
}

export function storageUploadScenario() {
  const context = getAuthContext('storage_auth_login');
  const postId = createAdoptionPostWithImages(context, 'storage_post_create');

  if (STORAGE_DELETE_CREATED) {
    deleteAdoptionPost(context, postId, 'storage_post_delete');
  }
}

export function storageMixedScenario() {
  const context = getAuthContext('storage_mixed_auth_login');
  const postId = createAdoptionPostWithImages(context, 'storage_mixed_post_create');

  if (!postId) {
    return;
  }

  updateAdoptionPostWithImages(context, postId, 'storage_mixed_post_update');
  deleteAdoptionPost(context, postId, 'storage_mixed_post_delete');
}
