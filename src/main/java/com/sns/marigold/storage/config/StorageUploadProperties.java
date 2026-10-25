package com.sns.marigold.storage.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 클라이언트 직접 업로드 설정.
 *
 * @param tokenSecret 업로드 토큰 HMAC 서명 키
 * @param urlTtl 업로드 URL 유효 시간
 * @param tokenTtl 업로드 토큰 유효 시간. 고아 정리 minimum-age보다 짧아야 한다.
 */
@ConfigurationProperties(prefix = "app.storage.upload")
public record StorageUploadProperties(
    String tokenSecret,
    @DefaultValue("15m") Duration urlTtl,
    @DefaultValue("1h") Duration tokenTtl) {}
