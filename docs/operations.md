# 실행과 운영

## 실행 프로필

| 프로필 | 주요 용도 | 데이터베이스와 저장소 | 개발 도구 |
| --- | --- | --- | --- |
| `local` | 로컬 개발 | MySQL 스키마 자동 갱신, 로컬 파일 저장 | Swagger 활성화 |
| `integration-test` | 통합 테스트 | Testcontainers MySQL, 테스트 설정 | Swagger 비활성화 |
| `prod` | 운영 | 기존 MySQL 스키마 사용, S3 저장 | Swagger 비활성화 |

`local` 프로필은 `application-local.yml`에 설정된 MySQL에 연결합니다. 실행 전에 접속 대상 데이터베이스(기본값: `marigold`)와 계정이 준비되어 있어야 합니다. 로컬 MySQL 서비스의 실행 및 종료는 수동으로 이뤄집니다.

아래는 Windows 서비스 예시입니다. `MySQL84`는 조회한 서비스명으로 바꾸고, 서비스 시작·종료는 관리자 권한 터미널에서 실행합니다.

```powershell
sc.exe query state= all | findstr /i mysql
net start MySQL84
net stop MySQL84
```


> [!WARNING]
> `local`의 `spring.jpa.hibernate.ddl-auto`는 `update`입니다. 스키마가 자동으로 변경될 수 있으므로 개발 전용 DB를 사용하세요. `prod`는 `none`이므로 스키마 변경은 배포 과정에서 별도로 적용해야 합니다.

## 환경 설정

공통 설정은 `application.yml`, 환경별 차이는 `application-local.yml`, `application-prod.yml`, `application-integration-test.yaml`에 있습니다. OAuth 설정은 모든 실행 프로필에 포함됩니다.

`.env.example`은 환경변수 템플릿이며 실제 사용하는 환경변수 값을 노출시키지 않기 위한 용도로 실제 사용하는 환경파일은 저장소에 없습니다. `.env.example` 파일의 필드값들을 적절하게 설정하여 `local` 프로필에서는 `.env.local` 파일을, `prod` 프로필에서는 `.env.prod` 파일을 준비해야 합니다. 환경변수 파일 경로는 각각 `application-local.yml`, `application-prod.yml`으로 설정합니다.

### 공통 인증과 OAuth2

| 변수 | 설명                                                                                                                            |
| --- |-------------------------------------------------------------------------------------------------------------------------------|
| `ACCESS_SECRET` | Access Token 서명 키입니다. 32 byte 이상의 임의 문자열을 사용합니다.                                                                              |
| `REFRESH_SECRET` | Refresh Token 서명 키입니다. 32 byte 이상의 임의 문자열을 사용하고 `ACCESS_SECRET`과 다른 값으로 설정합니다.                                                |
| `CSRF_SECRET` | CSRF token 서명 키입니다. 32 byte 이상이어야 합니다.                                                                                        |
| `BASE_URL` | backend base URL입니다. 예를 들면, OAuth2 callback 등록 주소는 `${BASE_URL}/oauth2/code/kakao`와 `${BASE_URL}/oauth2/code/naver`가 되는 식입니다. |
| `KAKAO_CLIENT_ID` | Kakao Developers에 등록한 OAuth2 client ID입니다.                                                                                    |
| `KAKAO_CLIENT_SECRET` | Kakao Developers에서 발급하고 활성화한 OAuth2 client secret입니다.                                                                         |
| `NAVER_CLIENT_ID` | Naver Developers에 등록한 OAuth2 client ID입니다.                                                                                    |
| `NAVER_CLIENT_SECRET` | Naver Developers에서 발급한 OAuth2 client secret입니다.                                                                               |

토큰 유효시간의 기본값은 `application.yml`에 설정되어 있으며 외부 설정으로 덮어쓸 수 있습니다.

### 데이터베이스와 frontend

| 변수 | 설명                                        |
| --- |-------------------------------------------|
| `DB_URL` | MySQL JDBC URL입니다.                        |
| `DB_USERNAME` | MySQL 접속 계정입니다. 로컬 기본값은 `root`입니다.        |
| `DB_PASSWORD` | MySQL 접속 비밀번호입니다. 로컬 기본값은 빈 문자열입니다.       |
| `FRONTEND_ORIGIN` | 프론트엔드 origin입니다. CORS 허용 origin으로 사용합니다.  |


### S3 저장소

다음 변수는 `prod` 프로필에서 필수이며 `local`에서는 사용하지 않습니다.

| 변수 | 설명 |
| --- | --- |
| `AWS_S3_BUCKET` | 업로드 파일을 저장할 S3 bucket 이름입니다. |
| `AWS_ACCESS_KEY` | S3 접근 권한이 있는 AWS access key ID입니다. |
| `AWS_SECRET_KEY` | 위 access key ID에 대응하는 AWS secret access key입니다. |
| `AWS_REGION` | S3 bucket이 위치한 AWS region code입니다. 예: `ap-northeast-2`. |


## 실행과 빌드

아래 Gradle 명령은 `back/` 디렉터리에서 실행하며 JDK 17이 필요합니다.

```powershell
# 로컬 실행
.\gradlew.bat bootRun --args='--spring.profiles.active=local'

# 단위 테스트
.\gradlew.bat test

# 컴포넌트 테스트
.\gradlew.bat componentTest

# 통합 테스트(Docker 필요)
.\gradlew.bat integrationTest

# 단위 테스트 + 컴포넌트 테스트 + 통합 테스트
.\gradlew.bat check

# 포맷 검사
.\gradlew.bat spotlessCheck

# 실행 JAR
.\gradlew.bat clean bootJar
```

테스트 코드는 `src/test/unit/java`, `src/test/component/java`, `src/test/integration/java`로 구분합니다. 각각 `test`, `componentTest`, `integrationTest`로 실행하며 `check`는 세 종류의 테스트와 포맷 검사를 모두 실행합니다.

`integrationTest`는 Testcontainers로 MySQL 8 컨테이너를 시작하므로 Docker가 필요합니다. 전용 설정은 `src/test/integration/resources/application-integration-test.yaml`에서 읽습니다.

## Docker 실행

아래 명령은 `back/` 디렉터리에서 실행합니다. `.env.example`을 참고해 `.env`를 준비하고 공통 인증·OAuth2, DB, frontend, S3의 운영 필수값을 채워야 합니다. `.env`는 커밋하지 않습니다.

```powershell
docker build -t marigold-backend .
docker run --rm -p 8080:8080 --env-file .env marigold-backend --spring.profiles.active=prod
```

Docker image는 Java 17에서 애플리케이션을 빌드하고 8080 port를 노출합니다. Image 자체에는 기본 활성 프로필이나 health check가 없으므로 실행 환경에서 지정해야 합니다.

## 배포

저장소의 workflow는 `main` branch push 시 GitHub Actions가 Docker image를 빌드해 ECR의 `marigold/server`에 `latest`와 commit SHA tag로 올리도록 설정되어 있습니다. 이후 SSH로 EC2의 `/home/ec2-user/app/deploy.sh`를 실행하는 구성입니다.

`deploy.sh`는 이 저장소에 없으므로 컨테이너 교체, 환경변수 주입과 rollback 방식은 EC2 배포 스크립트에서 별도로 관리됩니다. 현재 workflow에는 테스트나 포맷 검사 단계가 없습니다.

***중요 - 현재는 배포가 중단된 상태입니다. Github actions와 EC2, S3 모두 비활성화되었습니다. (update 2026.10.19)***


## 관측

애플리케이션은 다음 Actuator endpoint를 노출합니다.

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/prometheus`

애플리케이션이 Actuator 접근을 제한하지 않으므로 운영에서는 reverse proxy나 보안 그룹으로 외부 접근을 통제해야 합니다.

부하 테스트 시나리오, 테스트 데이터 준비와 로컬 Prometheus/Grafana 사용법은 [부하 테스트와 관측](load-testing.md)을 참고하세요. 해당 관측 구성은 부하 테스트용이며 운영 모니터링 배포를 대신하지 않습니다.
