package com.sns.marigold.auth.oauth2.handler;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.enums.AuthResponseCode;
import com.sns.marigold.auth.oauth2.enums.ProviderInfo;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 회원가입 전용 OAuth2 성공 Handler
 * 계정 존재 여부를 확인하고:
 * - 계정이 이미 존재하면: 에러 리다이렉트
 * - 계정이 없으면: 회원가입 수행 후 JWT 토큰 발급
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SignupSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {  
  private final Environment env;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();

    ProviderInfo providerInfo = principal.getOAuth2UserInfo().getProviderInfo();
    String providerId = principal.getOAuth2UserInfo().getName();


    log.info("회원가입 성공 - UserId: {}, Provider: {}, ProviderId: {}", principal.getUserId(), providerInfo, providerId);

    String redirectUrl = env.getProperty("url.frontend.auth.login");
    Objects.requireNonNull(redirectUrl, "url.frontend.auth.login is not configured");

    String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
    .queryParam("code", AuthResponseCode.SUCCESS.getCode())
    .build().toUriString();
    
    if (response.isCommitted()) {
      log.debug("응답이 이미 커밋되어 리다이렉트 할 수 없습니다. URL: {}", targetUrl);
      return;
    }

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}

