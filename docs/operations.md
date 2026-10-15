# 실행과 운영

## 실행 프로필

| 프로필 | 주요 용도 | 데이터베이스와 저장소 | 개발 도구 |
| --- | --- | --- | --- |
| `local` | 로컬 개발 | MySQL 스키마 재생성, 로컬 파일 저장 | Swagger 활성화 |
| `test` | 자동화 테스트 | Testcontainers MySQL, 테스트 설정 | 테스트에서 활성화 |
| `prod` | 운영 | 기존 MySQL 스키마 사용, S3 저장 | Swagger 비활성화 |

`local`은 `127.0.0.1:8080`, `prod`는 `0.0.0.0:8080`에서 수신합니다.

> [!WARNING]
> `local`의 `spring.jpa.hibernate.ddl-auto`는 `create`입니다. 기존 데이터를 보존해야 하는 DB에 연결하지 마세요. `prod`는 `none`이므로 스키마 변경은 배포 과정에서 별도로 적용해야 합니다.

## 환경 설정

공통 설정은 `application.yml`, 환경별 차이는 `application-local.yml`, `application-prod.yml`, `application-test.yaml`에 있습니다. OAuth 설정은 모든 실행 프로필에 포함됩니다.

로컬 프로필은 실행 위치를 기준으로 다음 파일을 선택적으로 읽습니다.

- `secrets/back/.env.local`
- `../secrets/back/.env.local`

`.env.example` 자체는 자동으로 로드하지 않습니다. 저장소 밖의 `../secrets/back/.env.local` 사용을 권장합니다.

필수 환경변수는 역할별로 관리합니다.

- 인증: `JWT_SECRET_KEY`, `CSRF_SECRET_KEY`, `BASE_URL`
- OAuth2: Kakao/Naver client ID와 client secret
- 데이터베이스: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- 프론트엔드: `FRONTEND_ORIGIN`
- 운영 저장소: S3 bucket, region, access key와 secret key

`BASE_URL`은 OAuth callback에 사용하는 외부 백엔드 URL이고, `FRONTEND_ORIGIN`은 CORS와 로그인 후 redirect에 사용하는 프론트엔드 origin입니다. 실제 변수 이름과 선택 설정은 `.env.example`을 기준으로 합니다.

## 실행과 빌드

```powershell
# 로컬 실행
.\gradlew.bat bootRun --args='--spring.profiles.active=local'

# 테스트와 포맷 검사
.\gradlew.bat test
.\gradlew.bat spotlessCheck

# 실행 JAR
.\gradlew.bat clean bootJar
```

통합 테스트는 Testcontainers로 MySQL 8 컨테이너를 시작하므로 Docker가 필요합니다.

```powershell
docker build -t marigold-backend .
docker run --rm -p 8080:8080 --env-file .env marigold-backend --spring.profiles.active=prod
```

Docker image는 Java 17에서 애플리케이션을 빌드하고 8080 port를 노출합니다. Image 자체에는 기본 활성 프로필이나 health check가 없으므로 실행 환경에서 지정해야 합니다.

## 배포

`main` branch push 시 GitHub Actions가 Docker image를 빌드해 ECR의 `marigold/server`에 `latest`와 commit SHA tag로 올립니다. 이후 SSH로 EC2의 `/home/ec2-user/app/deploy.sh`를 실행합니다.

`deploy.sh`는 이 저장소에 없으므로 컨테이너 교체, 환경변수 주입과 rollback 방식은 EC2 배포 스크립트에서 별도로 관리됩니다. 현재 workflow에는 테스트나 포맷 검사 단계가 없습니다.

## 관측과 부하 테스트

애플리케이션은 다음 Actuator endpoint를 노출합니다.

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/prometheus`

HTTP 요청 시간의 p50, p95, p99와 histogram을 수집합니다. 애플리케이션이 Actuator 접근을 제한하지 않으므로 운영에서는 reverse proxy나 보안 그룹으로 외부 접근을 통제해야 합니다.

`load-test/`에는 k6 시나리오와 로컬 Prometheus/Grafana 구성이 있습니다. 기본 smoke test 진입점은 다음과 같습니다.

```powershell
Set-Location load-test
Copy-Item .env.example .env
.\run.ps1 smoke
```

대상 환경과 테스트 계정을 `load-test/.env`에 설정하고 운영 데이터베이스에는 seed나 cleanup 스크립트를 실행하지 않습니다.

## 현재 운영 제약

- Recent-auth 상태는 애플리케이션 메모리에 저장됩니다. 재시작하면 사라지며 여러 인스턴스가 공유하지 않습니다.
- 운영 스키마는 애플리케이션이 생성하거나 갱신하지 않습니다.
- 로컬 Prometheus/Grafana 구성은 부하 테스트용이며 운영 모니터링 배포를 대신하지 않습니다.
