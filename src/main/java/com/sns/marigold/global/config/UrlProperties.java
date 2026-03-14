package com.sns.marigold.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.yml 파일에서 선언한 구조와 동일
// 외부 통신 URL
@ConfigurationProperties(prefix = "url")
public record UrlProperties(
    Frontend frontend,
    Backend backend
) {

  // 1. 프론트엔드 설정
  public record Frontend(
      String domain,
      String home,
      Auth auth
  ) {

    public record Auth(String login, String signup, String callback) {

    }
  }

  // 2. 백엔드 설정
  public record Backend(
      String apiPrefix,
      Auth auth
  ) {

    public record Auth(
        Login login,
        Signup signup
    ) {

      public record Login(String base, Endpoint endpoint, String redirection) {

        public record Endpoint(String base, String naver, String kakao) {

        }
      }

      public record Signup(String base, Endpoint endpoint, String redirection) {

        public record Endpoint(String base, String naver, String kakao) {

        }
      }
    }
  }
}