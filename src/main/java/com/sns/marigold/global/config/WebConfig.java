package com.sns.marigold.global.config;

import com.sns.marigold.global.util.TsidAnnotationFormatterFactory;
import com.sns.marigold.global.util.TsidFormatter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addFormatterForFieldAnnotation(new TsidAnnotationFormatterFactory());
    // @PathVariable/@RequestParam의 Long 타입 변환에 사용 (숫자 및 Base32 TSID 모두 지원)
    registry.addFormatter(new TsidFormatter());
  }
}
