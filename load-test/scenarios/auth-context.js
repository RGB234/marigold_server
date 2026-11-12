import { login } from './auth.js';

let cachedAuthContext = null;

export function getAuthContext(metricName = 'scenario_auth_login') {
  if (!cachedAuthContext || !cachedAuthContext.token) {
    const { token, targetUser } = login(null, metricName);
    cachedAuthContext = { token, user: targetUser };
  }

  if (!cachedAuthContext.token) {
    return null;
  }

  return cachedAuthContext;
}

export function authHeaders(context, headers = {}) {
  return {
    ...headers,
    Authorization: `Bearer ${context.token}`,
  };
}
