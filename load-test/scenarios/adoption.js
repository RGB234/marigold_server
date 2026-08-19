import http from 'k6/http';
import { check } from 'k6';
import { ADOPTION_PAGE_SIZE, API_VERSION, BASE_URL, selectAdoptionPage } from '../config.js';

export function getAdoptionPosts(metricName = 'adoption_list') {
  const page = selectAdoptionPage();
  const url = `${BASE_URL}${API_VERSION}/adoption?page=${page}&size=${ADOPTION_PAGE_SIZE}&sort=createdAt,desc`;
  const res = http.get(url, { tags: { name: metricName } });

  const isSuccessful = check(res, {
    'adoption list status is 200': (r) => r.status === 200,
    'adoption list success is true': (r) => r.status === 200 && Boolean(r.body) && r.json('success') === true,
  });

  let posts = [];
  if (isSuccessful) {
    try {
      posts = res.json('data.content') || [];
    } catch (e) {
      // JSON Parse fail
      throw new Error('Failed to parse JSON response');
    }
  }
  return posts;
}

export function getAdoptionPostDetail(postId, metricName = 'adoption_detail') {
  if (!postId) return;

  const url = `${BASE_URL}${API_VERSION}/adoption/${postId}`;
  const res = http.get(url, { tags: { name: metricName } });

  check(res, {
    'adoption detail status is 200': (r) => r.status === 200,
    'adoption detail success is true': (r) => r.status === 200 && Boolean(r.body) && r.json('success') === true,
  });
}

export function getAdoptionComments(postId, metricName = 'db_read_comments') {
  if (!postId) return;

  const url = `${BASE_URL}${API_VERSION}/adoption/${postId}/comments`;
  const res = http.get(url, { tags: { name: metricName } });

  check(res, {
    'adoption comments status is 200': (r) => r.status === 200,
    'adoption comments success is true': (r) => r.status === 200 && Boolean(r.body) && r.json('success') === true,
  });
}

export function getAdoptionPostsByWriter(userId, metricName = 'db_read_writer_posts') {
  if (!userId) return [];

  const url = `${BASE_URL}${API_VERSION}/adoption/writer/${userId}?page=0&size=${ADOPTION_PAGE_SIZE}&sort=createdAt,desc`;
  const res = http.get(url, { tags: { name: metricName } });

  const isSuccessful = check(res, {
    'writer adoption posts status is 200': (r) => r.status === 200,
    'writer adoption posts success is true': (r) => r.status === 200 && Boolean(r.body) && r.json('success') === true,
  });

  if (!isSuccessful) {
    return [];
  }

  try {
    return res.json('data.content') || [];
  } catch (e) {
    throw new Error('Failed to parse writer adoption posts response');
  }
}
