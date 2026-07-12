import http from 'k6/http';
import { check } from 'k6';
import { ADOPTION_PAGE_SIZE, API_VERSION, BASE_URL, selectAdoptionPage } from '../config.js';

export function getAdoptionPosts() {
  const page = selectAdoptionPage();
  const url = `${BASE_URL}${API_VERSION}/adoption?page=${page}&size=${ADOPTION_PAGE_SIZE}&sort=createdAt,desc`;
  const res = http.get(url);

  const isSuccessful = check(res, {
    'adoption list status is 200': (r) => r.status === 200,
    'adoption list success is true': (r) => r.json('success') === true,
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

export function getAdoptionPostDetail(postId) {
  if (!postId) return;

  const url = `${BASE_URL}${API_VERSION}/adoption/${postId}`;
  const res = http.get(url);

  check(res, {
    'adoption detail status is 200': (r) => r.status === 200,
    'adoption detail success is true': (r) => r.json('success') === true,
  });
}
