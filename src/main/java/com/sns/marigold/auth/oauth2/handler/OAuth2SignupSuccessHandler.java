package com.sns.marigold.auth.oauth2.handler;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.global.config.UrlProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 회원가입 전용 OAuth2 성공 Handler 계정 존재 여부를 확인하고: - 계정이 이미 존재하면: 에러 리다이렉트 - 계정이 없으면: 회원가입 수행 후 JWT 토큰 발급
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SignupSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
  private final UrlProperties urlProperties;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();

    log.info("회원가입 성공 - UserId: {}", principal.getUserId());

    String callbackUrl = urlProperties.frontend().auth().callback();
    Objects.requireNonNull(callbackUrl, "url.frontend.auth.callback is not configured");

    String redirectUrl =
        UriComponentsBuilder.fromUriString(callbackUrl)
            .queryParam("status", HttpStatus.CREATED.value())
            .build()
            .toUriString();

    if (response.isCommitted()) {
      log.debug("응답이 이미 커밋되어 리다이렉트 할 수 없습니다. URL: {}", redirectUrl);
      return;
    }

    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }
}
