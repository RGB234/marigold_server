package com.sns.marigold.global.error.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.global.error.ErrorSpec;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/** DispatcherServlet 밖의 필터와 Security 핸들러가 같은 오류 응답 정책을 사용하도록 합니다. */
@Component
@RequiredArgsConstructor
public class ProblemDetailWriter {

  private final ObjectMapper objectMapper;

  public void write(HttpServletRequest request, HttpServletResponse response, ErrorSpec error)
      throws IOException {
    ProblemDetail body = ProblemDetailFactory.create(error, request);
    response.setStatus(HttpErrorPolicy.statusOf(error).value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
