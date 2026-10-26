import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, API_VERSION, getRandomUser } from '../config.js';

export function login(user = null, metricName = 'auth_login') {
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
    tags: { name: metricName },
  };

  const res = http.post(url, payload, params);
  
  const isSuccessful = check(res, {
    'login status is 200': (r) => r.status === 200,
    'login returns access token': (r) => r.status === 200 && Boolean(r.body) && Boolean(r.json('accessToken')),
  });

  // LoginResponseDto: { accessToken, principal }. refresh·CSRF token은 쿠키와 X-CSRF-TOKEN 헤더로 온다.
  let token = null;
  let csrfToken = null;
  let refreshToken = null;
  if (isSuccessful) {
    try {
      token = res.json('accessToken');
      csrfToken = res.headers['X-CSRF-TOKEN'] || res.cookies['XSRF-TOKEN']?.[0]?.value || null;
      // Secure 쿠키라 http 대상에서는 쿠키 jar가 보내지 않으므로 WebSocket 핸드셰이크에 직접 싣는다.
      refreshToken = res.cookies.refresh_token?.[0]?.value || null;
    } catch (e) {
      // JSON 파싱 실패
    }
  }

  return { token, csrfToken, refreshToken, targetUser };
}
