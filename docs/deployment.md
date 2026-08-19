# Deployment

이 문서는 운영 배포에 필요한 최소 체크포인트를 정리합니다.

## Build

```powershell
cd back
.\gradlew.bat clean bootJar
```

Docker build:

```powershell
cd back
docker build -t marigold-backend .
```

Dockerfile은 Gradle로 boot jar를 만든 뒤 Java 17 JRE Alpine 이미지에서 실행합니다. 컨테이너는 `8080` 포트를 노출합니다.

## Run

운영 실행은 `prod` 프로필을 기준으로 합니다.

```powershell
docker run --rm -p 8080:8080 --env-file .env marigold-backend --spring.profiles.active=prod
```

배포 환경에 따라 `--spring.profiles.active=prod`는 컨테이너 실행 인자 대신 `SPRING_PROFILES_ACTIVE=prod` 환경변수로 제공해도 됩니다.

## 필수 환경변수

기본 예시는 [.env.example](../.env.example)을 기준으로 합니다.

| 변수 | 설명 |
| --- | --- |
| `JWT_SECRET_KEY` | JWT 서명 키 |
| `JWT_ACCESS_TOKEN_VALIDITY_SECONDS` | access token 만료 초. 미지정 시 기본값 `3600` |
| `JWT_REFRESH_TOKEN_VALIDITY_SECONDS` | refresh token 만료 초. 미지정 시 기본값 `86400` |
| `DB_URL` | MySQL JDBC URL |
| `DB_USERNAME` | MySQL 사용자 |
| `DB_PASSWORD` | MySQL 비밀번호 |
| `AWS_S3_BUCKET` | 업로드 bucket |
| `AWS_ACCESS_KEY` | AWS access key |
| `AWS_SECRET_KEY` | AWS secret key |
| `AWS_REGION` | AWS region |
| `BASE_URL` | OAuth2 redirect URI에 사용하는 백엔드 base URL |
| `FRONTEND_ORIGIN` | CORS와 redirect에 사용하는 프론트 origin |
| `KAKAO_CLIENT_ID` | Kakao OAuth2 client ID |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth2 client secret |
| `NAVER_CLIENT_ID` | Naver OAuth2 client ID |
| `NAVER_CLIENT_SECRET` | Naver OAuth2 client secret |

운영 환경변수는 CI/CD secret, secret manager, 또는 저장소 밖의 서버 전용 환경파일로 주입합니다. 환경파일을 사용할 경우 접근 권한을 제한하고 저장소에 커밋하지 않습니다.

## DB

운영 프로필은 MySQL과 `ddl-auto: none`을 사용합니다. 운영 DB schema 변경은 애플리케이션 자동 DDL에 맡기지 않습니다.

배포 전 확인할 항목:

- 애플리케이션이 접근 가능한 MySQL endpoint인지 확인
- schema와 index가 배포 버전과 맞는지 확인
- DB 계정 권한이 운영 정책에 맞는지 확인
- 테스트 데이터나 load-test 데이터가 운영 DB에 들어가지 않았는지 확인

## S3

S3는 업로드, 삭제, presigned URL 생성에 사용됩니다.

배포 전 확인할 항목:

- bucket region이 `AWS_REGION`과 일치하는지 확인
- 애플리케이션 credential에 필요한 최소 권한이 있는지 확인
- 운영 bucket에 load-test나 개발용 prefix를 섞지 않도록 확인
- presigned URL 만료 정책이 프론트 사용 흐름과 맞는지 확인

## OAuth2

OAuth2 provider에는 backend callback URL을 등록해야 합니다.

```text
{BASE_URL}/oauth2/code/kakao
{BASE_URL}/oauth2/code/naver
```

프론트 callback URL은 `FRONTEND_ORIGIN` 기반으로 계산됩니다.

```text
{FRONTEND_ORIGIN}/auth/callback
```

## CORS와 Cookie

서버는 `FRONTEND_ORIGIN`만 CORS origin으로 허용하고 credential 요청을 허용합니다.

인증 쿠키는 `Secure`, `SameSite=None`으로 발급됩니다. 운영에서는 HTTPS가 필요합니다.

## API 문서 노출

운영 프로필에서는 Swagger UI와 OpenAPI JSON을 비활성화합니다.

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

운영에서 API 명세가 필요하면 배포된 서버를 여는 대신 dev/staging 환경에서 OpenAPI JSON을 확인합니다.

## Observability

Actuator exposure:

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/prometheus`

운영 환경에서는 Actuator endpoint를 인터넷에 직접 공개하지 않습니다. `/actuator/prometheus`는 Prometheus scrape 대상에서만 접근 가능하도록 보안그룹, reverse proxy, 또는 네트워크 정책으로 제한합니다.

운영에서는 Prometheus scrape, 로그 수집, JVM/DB/S3 에러 알림을 함께 구성합니다.

## 배포 전 체크리스트

- `.\gradlew.bat test` 통과
- `.\gradlew.bat spotlessCheck` 통과
- `SPRING_PROFILES_ACTIVE=prod` 적용 확인
- JWT secret과 OAuth2 secret이 저장소에 커밋되지 않았는지 확인
- 운영 DB가 `ddl-auto: none`인지 확인
- Swagger가 운영에서 비활성화되어 있는지 확인
- `FRONTEND_ORIGIN`이 실제 프론트 origin과 일치하는지 확인
- OAuth2 provider callback URL이 `{BASE_URL}/oauth2/code/{provider}`와 일치하는지 확인
- S3 bucket, region, 권한 확인
- `/actuator/health` 확인
