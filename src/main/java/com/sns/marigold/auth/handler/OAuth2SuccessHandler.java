package com.sns.marigold.auth.handler;

import com.sns.marigold.auth.PersonalUserPrincipal;
import com.sns.marigold.user.entity.PersonalUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  final String REDIRECT_URL;

  public OAuth2SuccessHandler(@Value("${app.url.frontend.base}") String baseUrl,
    @Value("${app.url.frontend.main}") String mainUrl) {
    REDIRECT_URL = String.format("%s%s", baseUrl, mainUrl);
  }

  @Override
  public void onAuthenticationSuccess(
    HttpServletRequest request, HttpServletResponse response, Authentication authentication)
    throws IOException, ServletException {
    // OAuth2User 정보
    PersonalUserPrincipal userPrincipal = (PersonalUserPrincipal) authentication.getPrincipal();
    PersonalUser user = userPrincipal.getUser();

    assert user != null;
    String uid = user.getId().toString();
    String role = user.getRole().toString();

    // 세션에 최소한의 사용자 정보 저장
    HttpSession session = request.getSession(); // 세션 생성 (Tomcat)
    session.setAttribute("uid", uid);
    session.setAttribute("role", role);

    // JSON 응답 보내기
//    response.setContentType("application/json");
//    response.setCharacterEncoding("UTF-8");
//    response.setStatus(HttpServletResponse.SC_OK);
//
//    String json = String.format("{\"isAuthenticated\":true,\"uid\":\"%s\", \"role\": %s}", uid,
//      role);
//    response.getWriter().write(json);
//    response.getWriter().flush();

    // redirect
    response.sendRedirect(REDIRECT_URL);
  }
}
