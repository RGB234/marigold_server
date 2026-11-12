package com.sns.marigold.auth.common.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RefreshTokenTest {

  @Test
  void issuedValueParsesBackToTheSameToken() {
    RefreshToken issued = RefreshToken.issue("session-1");

    RefreshToken parsed = RefreshToken.parse(issued.value());

    assertThat(parsed).isEqualTo(issued);
    assertThat(parsed.secretHash()).isEqualTo(issued.secretHash());
  }

  @Test
  void eachIssueHasADifferentSecretAndHash() {
    RefreshToken first = RefreshToken.issue("session-1");
    RefreshToken second = RefreshToken.issue("session-1");

    assertThat(first.secret()).isNotEqualTo(second.secret());
    assertThat(first.secretHash()).isNotEqualTo(second.secretHash());
    assertThat(first.secretHash()).isNotEqualTo(first.secret());
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(
      strings = {
        "",
        "no-separator",
        ".AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        "session-1.too-short",
        // 이전에 발급한 JWT refresh token처럼 점이 더 있는 값
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.c2lnbmF0dXJl"
      })
  void malformedValuesAreRejected(String value) {
    assertThat(RefreshToken.parse(value)).isNull();
  }

  @Test
  void toStringDoesNotRevealTheSecret() {
    RefreshToken token = RefreshToken.issue("session-1");

    assertThat(token.toString()).contains("session-1").doesNotContain(token.secret());
  }
}
