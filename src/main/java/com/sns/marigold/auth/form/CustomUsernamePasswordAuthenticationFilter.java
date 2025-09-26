package com.sns.marigold.auth.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

public class CustomUsernamePasswordAuthenticationFilter extends
  UsernamePasswordAuthenticationFilter {

  private final ObjectMapper objectMapper = new ObjectMapper();

  public CustomUsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
    super.setAuthenticationManager(authenticationManager);
  }

  @Override
  public Authentication attemptAuthentication(HttpServletRequest request,
    HttpServletResponse response) throws AuthenticationException {
    try {
      // JSON 요청 처리
      Map<String, String> requestMap = objectMapper.readValue(request.getInputStream(), Map.class);
      String username = requestMap.get("username");
      String password = requestMap.get("password");

      UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
        username, password);
      setDetails(request, authRequest);

      return this.getAuthenticationManager().authenticate(authRequest);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
    FilterChain chain, Authentication authResult) throws IOException, ServletException {
    // 로그인 성공 시 처리
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write("{\"message\": \"로그인 성공\"}");
  }

  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request,
    HttpServletResponse response, AuthenticationException failed)
    throws IOException, ServletException {
    // 로그인 실패 시 처리
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write("{\"message\": \"로그인 실패\"}");
  }
}
