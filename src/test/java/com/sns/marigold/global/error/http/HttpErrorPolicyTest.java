package com.sns.marigold.global.error.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sns.marigold.adoption.exception.AdoptionError;
import com.sns.marigold.auth.exception.AuthError;
import com.sns.marigold.chat.exception.ChatError;
import com.sns.marigold.storage.exception.StorageError;

class HttpErrorPolicyTest {

  @Test
  void mapsSemanticFailureKindsWithoutDomainHttpDependencies() {
    assertThat(HttpErrorPolicy.statusOf(AuthError.UNAUTHORIZED)).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(HttpErrorPolicy.statusOf(AuthError.INVALID_CREDENTIALS))
        .isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(HttpErrorPolicy.statusOf(AdoptionError.POST_DELETED)).isEqualTo(HttpStatus.GONE);
    assertThat(HttpErrorPolicy.statusOf(ChatError.ROOM_CLOSED)).isEqualTo(HttpStatus.CONFLICT);
    assertThat(HttpErrorPolicy.statusOf(StorageError.FILE_TOO_LARGE))
        .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(HttpErrorPolicy.statusOf(StorageError.FILE_READ_FAILED))
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
