// package com.sns.marigold.auth.oauth2.handler;

// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import jakarta.servlet.http.HttpSession;
// import java.io.IOException;
// import lombok.extern.slf4j.Slf4j;

// import org.springframework.lang.NonNull;
// import org.springframework.stereotype.Component;
// import org.springframework.web.filter.OncePerRequestFilter;

// /**
//  * OAuth2 인증 시작 시 mode 파라미터를 세션에 저장하는 필터
//  * 회원가입 모드와 로그인 모드를 구분하기 위해 사용
//  * 
//  * 사용법:
//  * - 로그인: /oauth2/authorization/{provider}
//  * - 회원가입: /oauth2/authorization/{provider}?mode=signup
//  */
// @Component
// @Slf4j
// public class OAuth2ModeFilter extends OncePerRequestFilter {

//   private static final String MODE_PARAM = "mode";
//   private static final String MODE_SIGNUP = "signup";
//   private static final String SESSION_MODE_KEY = "oauth2_mode";
//   private static final String OAUTH2_AUTHORIZATION_PATH = "/oauth2/authorization/";

//   @Override
//   protected void doFilterInternal(
//       @NonNull HttpServletRequest request,
//       @NonNull HttpServletResponse response,
//       @NonNull FilterChain filterChain) throws ServletException, IOException {

//     String requestURI = request.getRequestURI();
    
//     // OAuth2 인증 시작 요청인지 확인
//     if (requestURI.startsWith(OAUTH2_AUTHORIZATION_PATH)) {
//       String mode = request.getParameter(MODE_PARAM);
      
//       if (MODE_SIGNUP.equals(mode)) {
//         // 회원가입 모드인 경우 세션에 저장
//         HttpSession session = request.getSession(true);
//         session.setAttribute(SESSION_MODE_KEY, MODE_SIGNUP);
//         log.debug("OAuth2 회원가입 모드로 세션에 저장: {}", session.getId());
//       }
//     }

//     filterChain.doFilter(request, response);
//   }

//   /**
//    * 세션에서 mode 값 가져오기 (사용 후 제거)
//    */
//   public static String getModeFromSession(HttpServletRequest request) {
//     HttpSession session = request.getSession(false);
//     if (session != null) {
//       String mode = (String) session.getAttribute(SESSION_MODE_KEY);
//       if (mode != null) {
//         // 사용 후 세션에서 제거
//         session.removeAttribute(SESSION_MODE_KEY);
//         return mode;
//       }
//     }
//     return null;
//   }
// }

