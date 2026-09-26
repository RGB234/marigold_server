package com.sns.marigold.global.error;

import java.net.URI;
import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;

import com.sns.marigold.global.error.dto.ErrorDetail;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProblemDetailFactory {

  private static final String TYPE_PREFIX = "urn:marigold:error:";

  public static ProblemDetail create(ErrorCode errorCode) {
    return create(errorCode, null);
  }

  public static ProblemDetail create(
      ErrorCode errorCode, @Nullable List<? extends ErrorDetail> errors) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(errorCode.getStatus(), errorCode.getMessage());
    problemDetail.setType(URI.create(TYPE_PREFIX + errorCode.getCode()));
    problemDetail.setTitle(errorCode.getStatus().getReasonPhrase());
    problemDetail.setProperty("errorCode", errorCode.getCode());
    if (errors != null && !errors.isEmpty()) {
      problemDetail.setProperty("errors", List.copyOf(errors));
    }
    return problemDetail;
  }
}
