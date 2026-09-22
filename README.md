# Marigold Backend

Marigold backend는 입양 게시글, 사용자, 인증, 채팅, 파일 저장소 기능을 제공하는 Spring Boot API 서버입니다.

## 기술 스택

| 영역 | 사용 기술 |
| --- | --- |
| Runtime | Java 17 |
| Framework | Spring Boot 3.5.3 |
| Build | Gradle Kotlin DSL |
| API | Spring MVC, Springdoc OpenAPI |
| Persistence | Spring Data JPA, MySQL |
| Security | Spring Security, OAuth2 Client, JWT, CSRF double-submit cookie |
| Realtime | WebSocket, STOMP, SockJS |
| Storage | Local filesystem, AWS S3 |
| Observability | Spring Boot Actuator, Micrometer, Prometheus |
| Test | JUnit 5, Spring Security Test, Testcontainers |
| Format | Spotless, google-java-format |

## 주요 문서

[전체 문서와 관리 규칙](docs/README.md)에서 주제별 문서를 찾습니다.

## 로컬 실행

로컬 실행은 `local` 프로필을 사용합니다. [설정 안내](docs/configuration.md)에 따라 [.env.example](.env.example)을 저장소 밖의 `../secrets/back/.env.local`에 복사하고 필수 값을 채웁니다. 기존 파일은 덮어쓰지 않습니다.

```powershell
cd back
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

`local` 프로필은 로컬 컴퓨터 서버, 로컬 MySQL, 로컬 파일 저장소를 함께 사용합니다. `prod` 프로필은 EC2, RDS, S3 운영 인프라를 사용합니다.

## 테스트와 포맷

```powershell
cd back
.\gradlew.bat test
.\gradlew.bat spotlessCheck
.\gradlew.bat spotlessApply
```

## API 문서 확인

API 상세 명세는 컨트롤러와 DTO의 OpenAPI 애노테이션을 기준으로 관리합니다. Swagger 실행 방법·주소, API prefix와 응답 규칙은 [API 가이드](docs/api-guide.md), WebSocket 연동은 [인증 흐름](docs/auth-flow.md)을 봅니다.

## 배포

Docker 빌드·실행 명령과 현재 자동 배포 범위는 [배포 문서](docs/deployment.md)를 확인합니다.
