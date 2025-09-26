package com.sns.marigold.auth.oauth2.handler;

import com.sns.marigold.auth.oauth2.PersonalUserPrincipal;
import com.sns.marigold.user.entity.PersonalUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

//  private final Environment env;

  @Override
  public void onAuthenticationSuccess(
    HttpServletRequest request, HttpServletResponse response, Authentication authentication)
    throws IOException, ServletException {
    // OAuth2User 정보
    PersonalUserPrincipal userPrincipal = (PersonalUserPrincipal) authentication.getPrincipal();
    PersonalUser user = userPrincipal.getUser();

    assert user != null;
    String uid = user.getId().toString();
//    String role = user.getRole().toString();

    // 세션
    HttpSession session = request.getSession(); // 세션 생성 (Tomcat)
    session.setAttribute("uid", uid);
//    session.setAttribute("role", role);

    // 응답
    response.setContentType("application/json; charset=UTF-8");
//    response.setContentType("application/json");
//    response.setCharacterEncoding("UTF-8");
    // 캐시 방지 헤더 추가
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache"); // HTTP/1.0 표준
    response.setHeader("Expires", "0");

    response.setStatus(HttpServletResponse.SC_OK);

    String jsonResponse = "{\"message\" : \"login success\"}";
    response.getWriter().write(jsonResponse);
    response.getWriter().flush();

    // redirect
//    response.sendRedirect(env.getProperty("app.url.frontend.main"));
  }
}
