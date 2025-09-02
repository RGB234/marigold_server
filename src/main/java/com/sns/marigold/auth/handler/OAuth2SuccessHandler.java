package com.sns.marigold.auth.handler;

import com.sns.marigold.global.enums.Role;
import com.sns.marigold.user.entity.UserEntity;
import com.sns.marigold.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final String SIGNUP_URL;
  private final String MAIN_URL;
  private final UserRepository userRepository;

  public OAuth2SuccessHandler(
      @Value("${url.base}") String baseURL,
      @Value("${url.path.signup}") String signUpPath,
      @Value("${url.path.main}") String mainPath,
      UserRepository userRepository) {
    this.userRepository = userRepository;
    this.SIGNUP_URL = baseURL + signUpPath;
    this.MAIN_URL = baseURL + mainPath;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {

    //        log.info("session :  {}", request.getSession().toString());

    OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
    String providerId = oAuth2User.getName();

    UserEntity user =
        userRepository
            .findByProviderId(providerId)
            .orElseThrow(
                () -> new EntityNotFoundException(("User not found with providerId : " + "###")));

    //        log.info("user provider id: {}", user.getProviderId());

    String redirectUrl = getRedirectUrlByRole(user.getRole(), providerId);
    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }

  private String getRedirectUrlByRole(Role role, String providerId) {
    //        if (role == Role.NOT_REGISTERED){
    if (role == Role.ROLE_NOT_REGISTERED) {
      return UriComponentsBuilder.fromUriString(SIGNUP_URL)
          //                .queryParam("providerId", providerId)
          .build()
          .toUriString();
    }

    return UriComponentsBuilder.fromUriString(MAIN_URL)
        //            .queryParam("providerId", providerId)
        .build()
        .toUriString();
  }
}
