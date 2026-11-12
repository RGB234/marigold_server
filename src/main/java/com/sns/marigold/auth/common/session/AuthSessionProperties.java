package com.sns.marigold.auth.common.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param refreshTtlSeconds refresh token 한 개의 유효시간. refresh할 때마다 이만큼 연장된다
 * @param absoluteTtlSeconds 로그인 후 세션이 유지되는 최대 시간. refresh로 연장해도 이 시각을 넘지 않는다
 * @param reuseGraceSeconds 직전 refresh token을 다시 받아 주는 시간. 응답을 받지 못한 재시도를 탈취로 오판하지 않게 한다
 */
@ConfigurationProperties(prefix = "app.auth.session")
public record AuthSessionProperties(
    long refreshTtlSeconds, long absoluteTtlSeconds, long reuseGraceSeconds) {}
