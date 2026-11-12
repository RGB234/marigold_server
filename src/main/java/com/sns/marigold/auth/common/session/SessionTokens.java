package com.sns.marigold.auth.common.session;

/**
 * @param refreshMaxAgeSeconds refresh 쿠키 유효시간. 세션 만료 시각까지 남은 시간이다
 */
public record SessionTokens(String accessToken, String refreshToken, long refreshMaxAgeSeconds) {}
