package com.sns.marigold.auth.common.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sns.marigold.global.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 로그아웃 성공 시 JSON 응답을 반환하는 핸들러.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        
        log.info("로그아웃 성공 핸들러 실행: JSON 응답 생성");

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Object> apiResponse = ApiResponse.success(HttpStatus.OK, "logout success");
        
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        log.info("로그아웃 성공 핸들러 실행 완료");
    }
}
