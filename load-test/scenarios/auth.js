import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, API_VERSION, getRandomUser } from '../config.js';

export function login(user = null) {
  const targetUser = user || getRandomUser();
  const url = `${BASE_URL}${API_VERSION}/auth/login`;
  
  const payload = JSON.stringify({
    email: targetUser.email,
    password: targetUser.password,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(url, payload, params);
  
  const isSuccessful = check(res, {
    'login status is 200': (r) => r.status === 200,
    'login has success field': (r) => r.json('success') === true,
  });

  // ApiResponse 구조 (global/dto/ApiResponse.java)
  // { success: true, message: "...", data: { accessToken: "..." } }
  let token = null;
  let csrfToken = null;
  if (isSuccessful) {
    try {
      token = res.json('data.accessToken');
      csrfToken = res.headers['X-CSRF-TOKEN'] || res.cookies['XSRF-TOKEN']?.[0]?.value || null;
    } catch (e) {
      // JSON 파싱 실패
    }
  }

  return { token, csrfToken, targetUser };
}
