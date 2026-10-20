package com.sns.marigold.auth.oauth2.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.user.exception.UserError;

class OAuth2FailureMapperTest {

  private final OAuth2FailureMapper mapper = new OAuth2FailureMapper();

  @Test
  void unknownProviderDescriptionIsNotExposed() {
    OAuth2AuthenticationException exception =
        new OAuth2AuthenticationException(
            new OAuth2Error("provider_error", "sensitive provider response", null));

    assertThat(mapper.toPublicError(exception)).isEqualTo(AuthError.OAUTH2_LOGIN_FAILURE);
  }

  @Test
  void registeredApplicationErrorKeepsItsPublicMeaning() {
    OAuth2AuthenticationException exception =
        new OAuth2AuthenticationException(
            new OAuth2Error(UserError.OAUTH2_ALREADY_LINKED.code(), "ignored", null));

    assertThat(mapper.toPublicError(exception)).isEqualTo(UserError.OAUTH2_ALREADY_LINKED);
  }
}
