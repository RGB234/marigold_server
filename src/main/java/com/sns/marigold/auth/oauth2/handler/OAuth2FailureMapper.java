package com.sns.marigold.auth.oauth2.handler;

import java.util.Arrays;
import java.util.stream.Stream;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.user.exception.UserError;

/** OAuth2 redirect에는 등록된 공개 오류만 전달합니다. */
@Component
public class OAuth2FailureMapper {

  public ErrorSpec toPublicError(AuthenticationException exception) {
    if (!(exception instanceof OAuth2AuthenticationException oauthException)) {
      return AuthError.OAUTH2_LOGIN_FAILURE;
    }

    String receivedCode = oauthException.getError().getErrorCode();
    return knownErrors()
        .filter(error -> error.code().equals(receivedCode))
        .findFirst()
        .orElse(AuthError.OAUTH2_LOGIN_FAILURE);
  }

  private Stream<ErrorSpec> knownErrors() {
    return Stream.concat(Arrays.stream(AuthError.values()), Arrays.stream(UserError.values()));
  }
}
