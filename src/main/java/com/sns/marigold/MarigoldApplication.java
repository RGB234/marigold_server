package com.sns.marigold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

@SpringBootApplication
@EnableJpaAuditing
@EnableSpringDataWebSupport(
    pageSerializationMode = PageSerializationMode.VIA_DTO) // PAGE 객체를 표준 DTO형태로 내려보냄
@ConfigurationPropertiesScan
public class MarigoldApplication {

  public static void main(String[] args) {
    SpringApplication.run(MarigoldApplication.class, args);
  }
}
