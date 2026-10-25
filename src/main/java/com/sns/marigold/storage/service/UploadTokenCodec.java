package com.sns.marigold.storage.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.storage.config.StorageUploadProperties;
import com.sns.marigold.storage.exception.StorageException;

/**
 * 업로드 토큰을 만들고 검증한다. 토큰은 {@code base64url(JSON 클레임).base64url(HMAC)} 형식이다.
 *
 * <p>서명으로 저장 키와 업로더, 용도를 묶어서, 다른 사용자의 파일이나 임의의 키를 도메인에 연결하지 못하게 한다.
 */
@Component
public class UploadTokenCodec {

  private static final String HMAC_ALGORITHM = "HmacSHA256";
  // 전역 ObjectMapper 설정(TSID 직렬화 등)에 영향받지 않도록 별도 인스턴스를 쓴다
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final StorageUploadProperties properties;
  private final Clock clock;

  @Autowired
  public UploadTokenCodec(StorageUploadProperties properties) {
    this(properties, Clock.systemUTC());
  }

  UploadTokenCodec(StorageUploadProperties properties, Clock clock) {
    if (!StringUtils.hasText(properties.tokenSecret())) {
      throw new IllegalStateException("upload token secret must not be blank");
    }
    this.properties = properties;
    this.clock = clock;
  }

  public String issue(
      String storedFileName,
      long userId,
      UploadPurpose purpose,
      String originalFileName,
      String contentType,
      long size) {
    long expiresAt = Instant.now(clock).plus(properties.tokenTtl()).getEpochSecond();
    UploadClaims claims =
        new UploadClaims(
            storedFileName, userId, purpose, originalFileName, contentType, size, expiresAt);
    String payload;
    try {
      payload = ENCODER.encodeToString(OBJECT_MAPPER.writeValueAsBytes(claims));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("failed to serialize upload token", e);
    }
    return payload + "." + ENCODER.encodeToString(hmac(payload));
  }

  /** 서명과 만료를 검증한 클레임을 돌려준다. 사용자와 용도 일치 여부는 호출자가 확인한다. */
  public UploadClaims verify(String token) {
    int separator = token == null ? -1 : token.indexOf('.');
    if (separator <= 0 || separator != token.lastIndexOf('.')) {
      throw StorageException.forInvalidUploadToken("malformed");
    }
    String payload = token.substring(0, separator);

    byte[] signature;
    try {
      signature = DECODER.decode(token.substring(separator + 1));
    } catch (IllegalArgumentException e) {
      throw StorageException.forInvalidUploadToken("malformed signature");
    }
    if (!MessageDigest.isEqual(hmac(payload), signature)) {
      throw StorageException.forInvalidUploadToken("signature mismatch");
    }

    UploadClaims claims;
    try {
      claims = OBJECT_MAPPER.readValue(DECODER.decode(payload), UploadClaims.class);
    } catch (IOException | IllegalArgumentException e) {
      throw StorageException.forInvalidUploadToken("malformed payload");
    }
    if (claims.expiresAt() < Instant.now(clock).getEpochSecond()) {
      throw StorageException.forInvalidUploadToken("expired");
    }
    return claims;
  }

  private byte[] hmac(String value) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(
          new SecretKeySpec(
              properties.tokenSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException("failed to sign upload token", e);
    }
  }

  public record UploadClaims(
      String storedFileName,
      long userId,
      UploadPurpose purpose,
      String originalFileName,
      String contentType,
      long size,
      long expiresAt) {}
}
