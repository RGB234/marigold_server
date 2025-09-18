package com.sns.marigold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MarigoldApplication {

  public static void main(String[] args) {
    SpringApplication.run(MarigoldApplication.class, args);
  }
}
