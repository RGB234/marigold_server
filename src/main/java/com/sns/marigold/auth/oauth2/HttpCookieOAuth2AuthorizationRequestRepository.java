package com.sns.marigold.auth.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.auth.common.util.CookieManager;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
  public static final String OAUTH2_ACTION_COOKIE_NAME = "oauth2_action";
  private static final int COOKIE_EXPIRE_SECONDS = 180;

  private final CookieManager cookieManager;

  private final ObjectMapper objectMapper;

  public HttpCookieOAuth2AuthorizationRequestRepository(
      CookieManager cookieManager, ObjectMapper objectMapper) {
    this.cookieManager = cookieManager;
    this.objectMapper = objectMapper;
  }

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    Cookie cookie = cookieManager.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
    if (cookie != null) {
      return deserialize(cookie.getValue(), OAuth2AuthorizationRequest.class);
    }
    return null;
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (authorizationRequest == null) {
      removeAuthorizationRequestCookies(request, response);
      return;
    }

    String serialized = serialize(authorizationRequest);
    cookieManager.addCookie(
        response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialized, COOKIE_EXPIRE_SECONDS);

    String action = request.getParameter("action");
    if (action != null) {
      cookieManager.addCookie(response, OAUTH2_ACTION_COOKIE_NAME, action, COOKIE_EXPIRE_SECONDS);
    }
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request, HttpServletResponse response) {
    OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);
    if (authorizationRequest != null) {
      removeAuthorizationRequestCookies(request, response);
    }
    return authorizationRequest;
  }

  public void removeAuthorizationRequestCookies(
      HttpServletRequest request, HttpServletResponse response) {
    cookieManager.expireCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
    cookieManager.expireCookie(response, OAUTH2_ACTION_COOKIE_NAME);
  }

  private String serialize(Object object) {
    try {
      byte[] bytes = objectMapper.writeValueAsBytes(object);
      return Base64.getUrlEncoder().encodeToString(bytes);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to serialize object", e);
    }
  }

  private <T> T deserialize(String cookieValue, Class<T> cls) {
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(cookieValue);
      return objectMapper.readValue(decoded, cls);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to deserialize object", e);
    }
  }
}
