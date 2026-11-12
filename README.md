# Marigold Backend

Marigold의 사용자 인증, 입양 게시글, 댓글, 채팅, 파일 저장을 담당하는 Spring Boot API 서버입니다.

## 기술 구성

- Java 17, Spring Boot 3, Gradle
- Spring Web, Spring Data JPA, MySQL
- Spring Security, JWT, Kakao/Naver OAuth2, Redis(인증 세션)
- SockJS/STOMP WebSocket
- 로컬 파일시스템 또는 Amazon S3
- Actuator, Micrometer, Prometheus

정확한 라이브러리 버전은 `build.gradle.kts`를 기준으로 합니다.

## 로컬 실행

필요한 도구는 JDK 17, MySQL 8과 Redis 7입니다. 로컬 Redis는 Docker로 실행하며 방법은 [실행과 운영](docs/operations.md#실행-프로필)에 있습니다. 통합 테스트를 실행하려면 Docker도 필요합니다.

PowerShell에서 환경 파일을 준비합니다.

```powershell
New-Item -ItemType Directory -Force ..\secrets\back | Out-Null
Copy-Item .env.example ..\secrets\back\.env.local
```

`../secrets/back/.env.local`에 Access token 비밀키, OAuth 클라이언트 정보와 로컬 DB 접속 정보를 입력합니다. 비밀키는 32바이트 이상이어야 합니다. `DB_URL`이 가리키는 데이터베이스는 미리 생성되어 있어야 합니다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

기동 후 다음 주소로 상태와 API 명세를 확인할 수 있습니다.

- Health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

> [!WARNING]
> `local` 프로필은 시작할 때 JPA 스키마를 자동으로 변경할 수 있습니다. 개발 전용 데이터베이스에만 연결하세요.

## 검사

```powershell
# 단위 테스트
.\gradlew.bat test

# 컴포넌트 테스트
.\gradlew.bat componentTest

# 통합 테스트(Docker 필요)
.\gradlew.bat integrationTest

# 포맷 검사
.\gradlew.bat spotlessCheck
```

검증 정책은 `ValidationPolicy.java`를 기준으로 관리합니다. 정책을 변경한 뒤 백엔드 JSON을 다시 생성합니다.

```powershell
.\gradlew.bat generateValidationPolicy
```

테스트는 `src/test` 아래에서 종류별로 관리하며, 각 `java` 폴더 안에서는 운영 코드의 package 구조를 유지합니다.

| 디렉터리 | 검증 범위 | 실행 명령 |
| --- | --- | --- |
| `src/test/unit/java` | 객체 로직, 직렬화·검증 규칙 | `test` |
| `src/test/component/java` | MVC·보안·트랜잭션 이벤트 구성, 실제 협력 객체를 연결한 인증·로컬 저장소 동작 | `componentTest` |
| `src/test/integration/java` | 애플리케이션 여러 계층과 실제 MySQL·Redis 연결 | `integrationTest` |

단일 객체를 직접 호출하고 협력 객체를 mock으로 대체하는 테스트는 `unit`, 제한된 기능의 실제 객체나 Spring 구성을 연결하는 테스트는 `component`에 둡니다. 컴포넌트 테스트는 임시 파일이나 내장 서버를 사용할 수 있지만 외부 DB·S3는 필요하지 않습니다.

`integrationTest`는 Testcontainers로 MySQL과 Redis 컨테이너를 시작하므로 Docker가 실행 중이어야 합니다. 공통 지원 코드는 `src/test/integration/java/com/sns/marigold/support`, 전용 설정은 `src/test/integration/resources/application-integration-test.yaml`에 있습니다. `check`를 실행하면 세 종류의 테스트와 포맷 검사를 모두 실행합니다.

## 문서

| 문서 | 내용 |
| --- | --- |
| [아키텍처](docs/architecture.md) | 시스템 경계, 패키지 책임, 데이터 모델, 주요 흐름과 현재 제약 |
| [파일 저장](docs/storage.md) | 파일 상태, 업로드 확정과 연결, 트랜잭션 경계, 고아 정리 |
| [실행과 운영](docs/operations.md) | 프로필, 환경변수, Docker·배포, S3 설정, 수동 스키마 변경, 관측 |
| [부하 테스트와 관측](docs/load-testing.md) | k6 시나리오, 테스트 데이터와 로컬 Prometheus/Grafana |

프론트엔드와 함께 지키는 계약은 `docs/contracts/`에 있습니다. 프론트엔드 문서도 이 파일들을 기준으로 삼습니다.

| 계약 | 내용 |
| --- | --- |
| [HTTP](docs/contracts/http.md) | REST 영역, 성공 응답, Problem Details 오류 |
| [인증](docs/contracts/auth.md) | 토큰·쿠키·CSRF, 인증 상태 복원, 유효시간, OAuth2 redirect |
| [업로드](docs/contracts/uploads.md) | 저장소 직접 업로드 절차와 업로드 오류 |
| [실시간](docs/contracts/realtime.md) | STOMP 경로, 단계별 검사, 오류 형식, 연결 종료와 재연결 |

수동으로 적용해야 하는 DB 변경은 `docs/migrations/`에 한 건당 한 파일로 둡니다. 상세 REST 계약의 기준은 로컬 프로필의 Swagger/OpenAPI입니다. 검증
규칙과 세부 동작은 코드와 테스트를 기준으로 합니다.
