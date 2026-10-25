package com.sns.marigold.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.sns.marigold.storage.config.StorageUploadProperties;
import com.sns.marigold.storage.exception.StorageError;
import com.sns.marigold.storage.exception.StorageException;
import com.sns.marigold.storage.service.UploadTokenCodec.UploadClaims;

class UploadTokenCodecTest {

  private static final Instant NOW = Instant.parse("2026-10-24T00:00:00Z");
  private static final StorageUploadProperties PROPERTIES =
      new StorageUploadProperties("test-secret", Duration.ofMinutes(15), Duration.ofHours(1));

  private final UploadTokenCodec codec = codecAt(NOW);

  @Test
  void issueAndVerify_RoundTrip() {
    String token = issueToken(codec);

    UploadClaims claims = codec.verify(token);

    assertThat(claims.storedFileName()).isEqualTo("adoption/post/key.png");
    assertThat(claims.userId()).isEqualTo(7L);
    assertThat(claims.purpose()).isEqualTo(UploadPurpose.ADOPTION_POST_IMAGE);
    assertThat(claims.originalFileName()).isEqualTo("cat.png");
    assertThat(claims.contentType()).isEqualTo("image/png");
    assertThat(claims.size()).isEqualTo(100L);
    assertThat(claims.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(1)).getEpochSecond());
  }

  @Test
  void verify_TamperedPayload() {
    String token = issueToken(codec);
    String otherPayload = issueToken(codecAt(NOW.plusSeconds(1))).split("\\.")[0];

    assertInvalid(otherPayload + "." + token.split("\\.")[1]);
  }

  @Test
  void verify_SignedWithOtherSecret() {
    UploadTokenCodec otherCodec =
        new UploadTokenCodec(
            new StorageUploadProperties("other-secret", PROPERTIES.urlTtl(), PROPERTIES.tokenTtl()),
            Clock.fixed(NOW, ZoneOffset.UTC));

    assertInvalid(issueToken(otherCodec));
  }

  @Test
  void verify_Expired() {
    String token = issueToken(codec);

    assertInvalid(codecAt(NOW.plus(Duration.ofHours(1)).plusSeconds(1)), token);
  }

  @Test
  void verify_Malformed() {
    assertInvalid(null);
    assertInvalid("");
    assertInvalid("no-separator");
    assertInvalid("a.b.c");
    assertInvalid("payload.!!!");
  }

  @Test
  void constructor_BlankSecret() {
    assertThatThrownBy(
            () ->
                new UploadTokenCodec(
                    new StorageUploadProperties(" ", PROPERTIES.urlTtl(), PROPERTIES.tokenTtl())))
        .isInstanceOf(IllegalStateException.class);
  }

  private String issueToken(UploadTokenCodec tokenCodec) {
    return tokenCodec.issue(
        "adoption/post/key.png",
        7L,
        UploadPurpose.ADOPTION_POST_IMAGE,
        "cat.png",
        "image/png",
        100L);
  }

  private void assertInvalid(String token) {
    assertInvalid(codec, token);
  }

  private void assertInvalid(UploadTokenCodec tokenCodec, String token) {
    assertThatThrownBy(() -> tokenCodec.verify(token))
        .isInstanceOf(StorageException.class)
        .extracting(e -> ((StorageException) e).getErrorSpec())
        .isEqualTo(StorageError.UPLOAD_TOKEN_INVALID);
  }

  private static UploadTokenCodec codecAt(Instant now) {
    return new UploadTokenCodec(PROPERTIES, Clock.fixed(now, ZoneOffset.UTC));
  }
}
