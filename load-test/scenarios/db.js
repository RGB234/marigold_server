import http from 'k6/http';
import { check } from 'k6';
import { API_VERSION, BASE_URL, getRandomUser } from '../config.js';
import {
  getAdoptionComments,
  getAdoptionPostDetail,
  getAdoptionPosts,
  getAdoptionPostsByWriter,
} from './adoption.js';
import { authHeaders, getAuthContext } from './auth-context.js';

function randomItem(items) {
  if (!items || items.length === 0) {
    return null;
  }
  return items[Math.floor(Math.random() * items.length)];
}

export function dbReadScenario() {
  const posts = getAdoptionPosts('db_read_adoption_list');
  const post = randomItem(posts);

  if (post) {
    getAdoptionPostDetail(post.id, 'db_read_adoption_detail');
    getAdoptionComments(post.id, 'db_read_comments');
  }

  getAdoptionPostsByWriter(getRandomUser().id, 'db_read_writer_posts');
}

export function updateAdoptionPostStatus(context, postId, status, metricName) {
  if (!context || !postId || !status) {
    return false;
  }

  const url = `${BASE_URL}${API_VERSION}/adoption/${postId}/status?status=${status}`;
  const res = http.patch(url, null, {
    headers: authHeaders(context),
    tags: { name: metricName },
  });

  return check(res, {
    [`${metricName} status is 200`]: (r) => r.status === 200,
    [`${metricName} success is true`]: (r) => r.status === 200 && Boolean(r.body) && r.json('success') === true,
  });
}

export function dbWriteScenario() {
  const context = getAuthContext('db_write_auth_login');
  if (!context) {
    return;
  }

  const posts = getAdoptionPostsByWriter(context.user.id, 'db_write_writer_posts');
  const post = randomItem(posts);
  if (!post) {
    return;
  }

  updateAdoptionPostStatus(context, post.id, 'RESERVED', 'db_write_status_reserved');
  updateAdoptionPostStatus(context, post.id, 'PROCEEDING', 'db_write_status_proceeding');
}

export function dbMixedReadScenario() {
  dbReadScenario();
}

export function dbMixedWriteScenario() {
  dbWriteScenario();
}
