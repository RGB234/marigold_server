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

운영 실행은 `prod` 프로필을 기준으로 합니다. `prod`는 EC2, RDS, S3 조합을 전제로 합니다.

```powershell
docker run --rm -p 8080:8080 --env-file .env marigold-backend --spring.profiles.active=prod
```

배포 환경에 따라 `--spring.profiles.active=prod`는 컨테이너 실행 인자 대신 `SPRING_PROFILES_ACTIVE=prod` 환경변수로 제공해도 됩니다.

## 환경변수 주입

변수 목록과 예시는 [.env.example](../.env.example), 환경별 필수 여부와 파일 주입 방법은 [설정 안내](configuration.md)를 기준으로 합니다.

운영 환경변수는 CI/CD secret, secret manager, 또는 저장소 밖의 서버 전용 환경파일로 주입합니다. 환경파일을 사용할 경우 접근 권한을 제한하고 저장소에 커밋하지 않습니다.

위 수동 실행의 `.env`는 실행 위치에 준비한 운영 환경파일입니다. 로컬 개발용 예제를 그대로 운영에 사용하지 않습니다.

## 현재 자동 배포 범위

[배포 workflow](../.github/workflows/deploy.yml)는 `main` push 시 ECR에 이미지를 올리고 EC2의 `/home/ec2-user/app/deploy.sh`를 실행합니다. 해당 서버 스크립트는 이 저장소에 없으므로 실제 이미지 선택·컨테이너 교체·환경변수 주입 방식은 서버에서 확인해야 합니다.

workflow의 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`는 배포 작업의 AWS 자격증명입니다. 애플리케이션의 `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`와 이름·용도가 다르며 자동으로 컨테이너에 전달되지 않습니다.

## DB

운영 프로필은 RDS MySQL과 `ddl-auto: none`을 사용합니다. 운영 DB schema 변경은 애플리케이션 자동 DDL에 맡기지 않습니다.

배포 전 확인할 항목:

- 애플리케이션이 접근 가능한 MySQL endpoint인지 확인
- schema와 index가 배포 버전과 맞는지 확인
- DB 계정 권한이 운영 정책에 맞는지 확인
- 테스트 데이터나 load-test 데이터가 운영 DB에 들어가지 않았는지 확인

## S3

`prod` 프로필의 파일 저장소는 S3입니다. 업로드, 삭제, presigned URL 생성에 사용됩니다.

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

배포 주소를 [인증 흐름의 CORS·쿠키 규칙](auth-flow.md)에 맞춥니다. 운영에서는 HTTPS를 사용하고 `FRONTEND_ORIGIN`에는 경로 없이 scheme·host·port로 구성한 origin을 지정합니다.

## API 문서 노출

운영 프로필에서는 Swagger UI와 OpenAPI JSON을 비활성화합니다.

설정 원본은 [application-prod.yml](../src/main/resources/application-prod.yml)입니다.

운영에서 API 명세가 필요하면 배포된 서버를 여는 대신 `local` 또는 별도 staging 환경에서 OpenAPI JSON을 확인합니다.

## Observability

Actuator exposure:

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/prometheus`

위 목록은 애플리케이션이 노출하도록 설정한 endpoint입니다. 현재 공통 SecurityFilterChain의 URL 인가는 `permitAll`이므로 운영에서 접근 제한이 적용됐다고 간주하면 안 됩니다. `/actuator/prometheus` 등은 보안그룹, reverse proxy, 또는 네트워크 정책으로 접근 범위를 제한하고 실제 접근 여부를 확인합니다.

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
