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

  // LoginResponseDto: { accessToken, principal }. 이후 API와 STOMP CONNECT는 access token만으로 인증한다.
  let token = null;
  if (isSuccessful) {
    try {
      token = res.json('accessToken');
    } catch (e) {
      // JSON 파싱 실패
    }
  }

  return { token, targetUser };
}
