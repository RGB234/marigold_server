package com.sns.marigold.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sns.marigold.adoption.exception.AdoptionError;
import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.chat.exception.ChatError;
import com.sns.marigold.storage.exception.StorageError;
import com.sns.marigold.user.exception.UserError;

class ErrorCatalogTest {

  @Test
  void wireCodesAreUniqueAndPublicMessagesAreSafeFallbacks() {
    List<ErrorSpec> errors = allErrors();

    assertThat(errors).extracting(ErrorSpec::code).doesNotHaveDuplicates();
    assertThat(errors)
        .allSatisfy(
            error -> {
              assertThat(error.code()).matches("[A-Z][A-Z0-9_]*");
              assertThat(error.publicMessage()).isNotBlank();
              assertThat(error.kind()).isNotNull();
            });
  }

  private List<ErrorSpec> allErrors() {
    List<ErrorSpec> errors = new ArrayList<>();
    errors.addAll(List.of(CommonError.values()));
    errors.addAll(List.of(AuthError.values()));
    errors.addAll(List.of(UserError.values()));
    errors.addAll(List.of(AdoptionError.values()));
    errors.addAll(List.of(ChatError.values()));
    errors.addAll(List.of(StorageError.values()));
    return errors;
  }
}
