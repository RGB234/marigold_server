package com.sns.marigold.auth.oauth2.handler;

import com.sns.marigold.auth.common.CustomPrincipal;
import com.sns.marigold.auth.common.jwt.JwtManager;
import com.sns.marigold.auth.common.util.CookieManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtManager jwtManager;
    private final CookieManager cookieManager;
    
    @Value("${url.frontend.home}")
    private String frontendHomeUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        
        CustomPrincipal principal = (CustomPrincipal) authentication.getPrincipal();
        log.info("로그인 성공 - UserId: {}", principal.getUserId());

        // 1. JWT 토큰 생성
        String accessToken = jwtManager.createAccessToken(principal);
        String refreshToken = jwtManager.createRefreshToken(principal);

        // 2. 쿠키 설정 및 추가
        cookieManager.addCookie(response, cookieManager.ACCESS_TOKEN_NAME, accessToken, jwtManager.getAccessTokenValidityInMilliseconds());
        cookieManager.addCookie(response, cookieManager.REFRESH_TOKEN_NAME, refreshToken, jwtManager.getRefreshTokenValidityInMilliseconds());

        // 3. 리다이렉트 처리
        if (response.isCommitted()) {
            log.debug("응답이 이미 커밋되어 리다이렉트 할 수 없습니다.");
            return;
        }
        getRedirectStrategy().sendRedirect(request, response, frontendHomeUrl);
    }
}