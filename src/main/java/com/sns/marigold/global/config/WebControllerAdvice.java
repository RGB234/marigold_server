package com.sns.marigold.global.config;

import java.beans.PropertyEditorSupport;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.util.HtmlUtils;

/**
 * 전역 컨트롤러 바인딩 설정을 담당하는 Advice 클래스입니다.
 * 모든 문자열 입력에 대해 XSS 방지를 위한 HTML 이스케이프 처리를 수행합니다.
 *  ModelAttribute (Form 데이터), RequestParam (쿼리 스트링) 등 Servlet 기반의 데이터 바인딩 담당.
 */
@ControllerAdvice
public class WebControllerAdvice {

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(String.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String text) {
        // null이 아닐 경우 HTML 이스케이프 및 trim() 처리
        setValue(text == null ? null : HtmlUtils.htmlEscape(text.trim()));
      }
    });
  }
}
