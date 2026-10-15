# Marigold Backend

Marigold의 사용자 인증, 입양 게시글, 댓글, 채팅, 파일 저장을 담당하는 Spring Boot API 서버입니다.

## 기술 구성

- Java 17, Spring Boot 3, Gradle
- Spring Web, Spring Data JPA, MySQL
- Spring Security, JWT, Kakao/Naver OAuth2
- SockJS/STOMP WebSocket
- 로컬 파일시스템 또는 Amazon S3
- Actuator, Micrometer, Prometheus

정확한 라이브러리 버전은 `build.gradle.kts`를 기준으로 합니다.

## 로컬 실행

필요한 도구는 JDK 17과 MySQL 8입니다. 통합 테스트를 실행하려면 Docker도 필요합니다.

PowerShell에서 환경 파일을 준비합니다.

```powershell
New-Item -ItemType Directory -Force ..\secrets\back | Out-Null
Copy-Item .env.example ..\secrets\back\.env.local
```

`../secrets/back/.env.local`에 JWT·CSRF 비밀키, OAuth 클라이언트 정보와 로컬 DB 접속 정보를 입력합니다. 두 비밀키는 각각 32바이트 이상의 서로 다른 값이어야 하며, `DB_URL`이 가리키는 데이터베이스는 미리 생성되어 있어야 합니다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

기동 후 다음 주소로 상태와 API 명세를 확인할 수 있습니다.

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

> [!WARNING]
> `local` 프로필은 시작할 때 JPA 스키마를 다시 생성합니다. 폐기 가능한 로컬 데이터베이스에만 연결하세요.

## 검사

```powershell
.\gradlew.bat test
.\gradlew.bat spotlessCheck
```

검증 정책은 `ValidationPolicy.java`를 기준으로 관리합니다. 정책을 변경한 뒤 백엔드 JSON을 다시 생성합니다.

```powershell
.\gradlew.bat generateValidationPolicy
```

통합 테스트는 Testcontainers로 MySQL 컨테이너를 시작하므로 Docker가 실행 중이어야 합니다.

## 문서

- [아키텍처](docs/architecture.md): 시스템 경계, 패키지 책임과 핵심 데이터 흐름
- [인터페이스](docs/interfaces.md): REST, 인증, 오류와 WebSocket 계약
- [실행과 운영](docs/operations.md): 프로필, 환경변수, 배포와 관측

상세 REST 계약의 기준은 로컬 프로필의 Swagger/OpenAPI입니다. 검증 규칙과 세부 동작은 코드와 테스트를 기준으로 합니다.
