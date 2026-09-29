package com.sns.marigold.global.error.http;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.Nullable;

import com.sns.marigold.global.error.ErrorSpec;
import com.sns.marigold.global.error.dto.ValidationViolation;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProblemDetailFactory {

  private static final String TYPE_PREFIX = "urn:marigold:error:";

  public static ProblemDetail create(ErrorSpec error) {
    return create(error, HttpErrorPolicy.statusOf(error), null, null);
  }

  public static ProblemDetail create(ErrorSpec error, @Nullable HttpServletRequest request) {
    return create(error, HttpErrorPolicy.statusOf(error), request, null);
  }

  public static ProblemDetail create(
      ErrorSpec error,
      HttpStatusCode status,
      @Nullable HttpServletRequest request,
      @Nullable List<ValidationViolation> errors) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, error.publicMessage());
    problemDetail.setType(URI.create(TYPE_PREFIX + error.code()));
    HttpStatus httpStatus = HttpStatus.resolve(status.value());
    problemDetail.setTitle(
        httpStatus == null ? "HTTP " + status.value() : httpStatus.getReasonPhrase());
    problemDetail.setProperty("errorCode", error.code());

    if (request != null) {
      problemDetail.setInstance(URI.create(request.getRequestURI()));
      String requestId = RequestIdFilter.find(request);
      if (requestId != null) {
        problemDetail.setProperty("requestId", requestId);
      }
    }
    if (errors != null && !errors.isEmpty()) {
      problemDetail.setProperty("errors", List.copyOf(errors));
    }
    return problemDetail;
  }
}
