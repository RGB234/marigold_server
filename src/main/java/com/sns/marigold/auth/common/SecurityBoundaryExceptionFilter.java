package com.sns.marigold.auth.common;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sns.marigold.global.error.CommonError;
import com.sns.marigold.global.error.FailureReporter;
import com.sns.marigold.global.error.exception.ApplicationException;
import com.sns.marigold.global.error.http.ProblemDetailWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** MVC 예외 처리기에 도달하지 않는 Security 필터 오류를 공통 HTTP 오류 계약으로 변환합니다. */
@Component
@RequiredArgsConstructor
public class SecurityBoundaryExceptionFilter extends OncePerRequestFilter {

  private final ProblemDetailWriter problemDetailWriter;
  private final FailureReporter failureReporter;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (ApplicationException exception) {
      if (response.isCommitted()) {
        throw exception;
      }
      failureReporter.report(exception, "security-filter");
      response.resetBuffer();
      problemDetailWriter.write(request, response, exception.getErrorSpec());
    } catch (Exception exception) {
      if (response.isCommitted()) {
        rethrow(exception);
      }
      failureReporter.reportUnhandled(exception, "security-filter");
      response.resetBuffer();
      problemDetailWriter.write(request, response, CommonError.INTERNAL_SERVER_ERROR);
    }
  }

  private void rethrow(Exception exception) throws ServletException, IOException {
    if (exception instanceof IOException ioException) {
      throw ioException;
    }
    if (exception instanceof ServletException servletException) {
      throw servletException;
    }
    if (exception instanceof RuntimeException runtimeException) {
      throw runtimeException;
    }
    throw new ServletException(exception);
  }
}
