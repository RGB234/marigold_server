package com.sns.marigold.auth.common;

import java.time.Instant;

/** access token 인증 객체의 details. WebSocket 연결이 세션 폐기와 token 만료에 맞춰 끊기도록 둘을 함께 전달합니다. */
public record AccessTokenDetails(String sessionId, Instant expiresAt) {}
