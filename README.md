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

- [API 가이드](docs/api-guide.md)
- [인증 흐름](docs/auth-flow.md)
- [아키텍처](docs/architecture.md)
- [배포](docs/deployment.md)
- [검증 정책](docs/validation-policy.md)
- [부하 테스트](load-test/README.md)

## 로컬 실행

로컬 실행은 `local` 프로필을 사용합니다. 기본값은 로컬 MySQL과 `./uploads` 저장소를 바라보며, 값이 다르면 [.env.example](.env.example)을 기준으로 환경변수를 준비합니다.

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

API 상세 명세는 컨트롤러와 DTO의 OpenAPI 애노테이션을 기준으로 관리합니다.

기본 설정에서는 `springdoc`이 꺼져 있으므로 Swagger UI를 확인할 때는 `local` 프로필로 실행합니다.

```powershell
cd back
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

운영 프로필에서는 Swagger와 OpenAPI JSON을 노출하지 않습니다.

## API Prefix

모든 REST API는 `/api/v1` 아래에 있습니다.

| 도메인 | Base path |
| --- | --- |
| Auth | `/api/v1/auth` |
| User | `/api/v1/user` |
| Adoption | `/api/v1/adoption` |
| Chat | `/api/v1/chat` |

WebSocket endpoint는 `/ws`이고, STOMP publish prefix는 `/pub`, subscribe prefix는 `/sub`입니다.

## 배포

Docker 이미지는 [Dockerfile](Dockerfile)로 빌드합니다.

```powershell
cd back
docker build -t marigold-backend .
docker run --rm -p 8080:8080 --env-file .env marigold-backend
```

운영 배포 전 필요한 설정은 [배포 문서](docs/deployment.md)를 확인합니다.
