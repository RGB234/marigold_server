import { login } from './auth.js';

let cachedAuthContext = null;

export function getAuthContext(metricName = 'scenario_auth_login') {
  if (!cachedAuthContext || !cachedAuthContext.token || !cachedAuthContext.csrfToken) {
    const { token, csrfToken, targetUser } = login(null, metricName);
    cachedAuthContext = { token, csrfToken, user: targetUser };
  }

  if (!cachedAuthContext.token || !cachedAuthContext.csrfToken) {
    return null;
  }

  return cachedAuthContext;
}

export function authHeaders(context, headers = {}) {
  return {
    ...headers,
    Authorization: `Bearer ${context.token}`,
    'X-CSRF-TOKEN': context.csrfToken,
    Cookie: `XSRF-TOKEN=${context.csrfToken}`,
  };
}
