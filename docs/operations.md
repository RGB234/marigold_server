# 실행과 운영

## 실행 프로필

| 프로필 | 주요 용도 | 데이터베이스와 저장소 | 개발 도구 |
| --- | --- | --- | --- |
| `local` | 로컬 개발 | MySQL 스키마 자동 갱신, 로컬 Redis, 로컬 파일 저장 | Swagger 활성화 |
| `integration-test` | 통합 테스트 | Testcontainers MySQL·Redis, 테스트 설정 | Swagger 비활성화 |
| `prod` | 운영 | 기존 MySQL 스키마 사용, 외부 Redis, S3 저장 | Swagger 비활성화 |

`local` 프로필은 `application-local.yml`에 설정된 MySQL에 연결합니다. 실행 전에 접속 대상 데이터베이스(기본값: `marigold`)와 계정이 준비되어 있어야 합니다. 로컬 MySQL 서비스의 실행 및 종료는 수동으로 이뤄집니다.

아래는 Windows 서비스 예시입니다. `MySQL84`는 조회한 서비스명으로 바꾸고, 서비스 시작·종료는 관리자 권한 터미널에서 실행합니다.

```powershell
sc.exe query state= all | findstr /i mysql
net start MySQL84
net stop MySQL84
```

인증 세션을 저장하는 Redis도 실행 중이어야 합니다. Redis는 공식 Windows 빌드가 없으므로 Docker로 실행합니다. 기본 접속 대상은
`localhost:6379`입니다.

```powershell
docker run -d --name marigold-redis -p 6379:6379 redis:7.4-alpine
docker start marigold-redis
docker stop marigold-redis
```


> [!WARNING]
> `local`의 `spring.jpa.hibernate.ddl-auto`는 `update`입니다. 스키마가 자동으로 변경될 수 있으므로 개발 전용 DB를 사용하세요. `prod`는 `none`이므로 스키마 변경은 배포 과정에서 별도로 적용해야 합니다([수동 스키마 변경](#수동-스키마-변경)).

## 환경 설정

공통 설정은 `application.yml`, 환경별 차이는 `application-local.yml`, `application-prod.yml`, `application-integration-test.yaml`에 있습니다. OAuth 설정은 모든 실행 프로필에 포함됩니다.

`.env.example`은 환경변수 템플릿이며 실제 사용하는 환경변수 값을 노출시키지 않기 위한 용도로 실제 사용하는 환경파일은 저장소에 없습니다. `.env.example` 파일의 필드값들을 적절하게 설정하여 `local` 프로필에서는 `.env.local` 파일을, `prod` 프로필에서는 `.env.prod` 파일을 준비해야 합니다. 환경변수 파일 경로는 각각 `application-local.yml`, `application-prod.yml`으로 설정합니다.

변수 목록의 원본은 `.env.example`입니다. 변수를 추가하거나 지우면 `.env.example`과 아래 표를 함께 고칩니다.

### 공통 인증과 OAuth2

| 변수 | 설명                                                                                                                            |
| --- |-------------------------------------------------------------------------------------------------------------------------------|
| `ACCESS_SECRET` | Access Token 서명 키입니다. 32 byte 이상의 임의 문자열을 사용합니다.                                                                              |
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
| `FRONTEND_ORIGIN` | 프론트엔드 origin입니다. CORS 허용 origin으로 사용합니다. `BASE_URL`과 같은 사이트(같은 등록 도메인과 scheme)여야 인증 쿠키가 전송됩니다([인증 계약](contracts/auth.md#토큰과-csrf)). `*.workers.dev`·`*.pages.dev` 같은 공용 도메인은 서브도메인마다 다른 사이트이므로 사용자 지정 도메인을 씁니다. |

### Redis

인증 세션을 저장합니다. 인증이 필요한 요청마다 조회하므로, Redis가 응답하지 않으면 해당 요청은 2초 안에 `503`으로 실패합니다
([인증 계약](contracts/auth.md#세션-저장소-장애)).

| 변수 | 설명 |
| --- | --- |
| `REDIS_HOST` | Redis host입니다. 로컬 기본값은 `localhost`이며 `prod`에서는 필수입니다. |
| `REDIS_PORT` | Redis port입니다. 기본값은 `6379`입니다. |
| `REDIS_PASSWORD` | Redis 비밀번호입니다. 비어 있으면 인증 없이 접속합니다. |

TLS가 필요한 Redis(예: 전송 중 암호화를 켠 ElastiCache)는 `spring.data.redis.ssl.enabled=true`도 설정해야 합니다.

#### 운영 Redis 설정

인증 세션은 Redis에만 있으므로, Redis가 데이터를 버리면 그 세션의 사용자는 로그아웃됩니다. `prod` Redis는 아래 두 가지를 지켜야 합니다.
로컬 Docker Redis는 기본값으로 충분합니다.

**메모리 정책은 `maxmemory-policy noeviction`으로 둡니다.**

- 세션 키는 모두 TTL이 있어서, `volatile-*`나 `allkeys-*` 정책이면 메모리가 찼을 때 살아 있는 세션이 지워지고 사용자가 무작위로
  로그아웃됩니다.
- `noeviction`이면 메모리가 찼을 때 새 쓰기만 실패합니다. 새 로그인과 refresh는 `503 AUTH_SESSION_STORE_UNAVAILABLE`로 실패하고,
  이미 있는 세션 확인은 계속 됩니다. `used_memory`를 지켜보고 메모리를 여유 있게 잡습니다. 세션 하나는 수백 바이트입니다.
- Redis 자체의 기본값은 `noeviction`이지만 ElastiCache 기본 파라미터 그룹은 `volatile-lru`입니다. ElastiCache를 쓰면 사용자 지정
  파라미터 그룹을 만들어 바꿉니다.

**재시작과 장애에도 세션이 남도록 영속성을 켭니다.**

- 디스크나 복제본에 남기지 않으면 Redis가 재시작되거나 죽을 때 모든 세션이 사라져 전체 사용자가 다시 로그인해야 합니다.
- 직접 운영하는 Redis는 AOF(`appendonly yes`, `appendfsync everysec`)를 켜고, 데이터 디렉터리를 컨테이너 밖 볼륨에 둡니다.
- ElastiCache는 복제본과 Multi-AZ 자동 장애 조치를 켭니다.
- 어느 쪽이든 마지막 약 1초(AOF `everysec`)나 복제 지연만큼의 쓰기는 잃을 수 있습니다. 그 사이에 refresh한 세션은 서버에 이전 token이
  남아, 다음 refresh가 재사용으로 판정되어 폐기됩니다. 해당 사용자만 다시 로그인하면 됩니다.

직접 운영하는 Redis는 다음으로 확인합니다. ElastiCache는 `CONFIG` 명령이 막혀 있으므로 파라미터 그룹에서 확인합니다.

```bash
redis-cli CONFIG GET maxmemory-policy
redis-cli CONFIG GET appendonly
```


### S3 저장소

다음 변수는 `prod` 프로필에서 필수이며 `local`에서는 사용하지 않습니다.

| 변수 | 설명 |
| --- | --- |
| `AWS_S3_BUCKET` | 업로드 파일을 저장할 S3 bucket 이름입니다. |
| `AWS_ACCESS_KEY` | S3 접근 권한이 있는 AWS access key ID입니다. |
| `AWS_SECRET_KEY` | 위 access key ID에 대응하는 AWS secret access key입니다. |
| `AWS_REGION` | S3 bucket이 위치한 AWS region code입니다. 예: `ap-northeast-2`. |
| `UPLOAD_TOKEN_SECRET` | 클라이언트 직접 업로드 토큰의 서명 키입니다. 32 byte 이상의 임의 문자열을 사용합니다. 비어 있으면 기동에 실패합니다. |


## 실행과 빌드

로컬 실행과 테스트 명령은 [README](../README.md)에 있습니다. 실행 JAR는 `back/` 디렉터리에서 다음 명령으로 만듭니다(JDK 17 필요).

```powershell
.\gradlew.bat clean bootJar
```

## Docker 실행

아래 명령은 `back/` 디렉터리에서 실행합니다. `.env.example`을 참고해 `.env.prod`를 준비하고 공통 인증·OAuth2, DB, frontend, Redis, S3의 운영 필수값을 채워야 합니다. `.env.prod`는 커밋하지 않습니다.

`application-prod.yml`은 `spring.config.import`에 적힌 경로의 환경파일을 `optional:` 없이 읽으므로, 그 파일이 없으면 기동에 실패합니다. `--env-file`로 값을 넘기는 것만으로는 부족하며, 컨테이너 안의 같은 경로에 파일을 마운트해야 합니다.

```powershell
docker build -t marigold-backend .
docker run --rm -p 8080:8080 -v <호스트의 .env.prod 경로>:<application-prod.yml의 import 경로>:ro marigold-backend --spring.profiles.active=prod
```

Docker image는 Java 17에서 애플리케이션을 빌드하고 8080 port를 노출합니다. Image 자체에는 기본 활성 프로필이나 health check가 없으므로 실행 환경에서 지정해야 합니다.

## 배포

저장소의 workflow는 `main` branch push 시 GitHub Actions가 먼저 `./gradlew check`(단위·컴포넌트·통합 테스트와 Spotless 포맷 검사)를 실행하고, 통과한 경우에만 Docker image를 빌드해 ECR의 `marigold/server`에 `latest`와 commit SHA tag로 올리도록 설정되어 있습니다. 이후 SSH로 EC2의 `/home/ec2-user/app/deploy.sh`를 실행하는 구성입니다.

`deploy.sh`는 이 저장소에 없으므로 컨테이너 교체, 환경변수 주입과 rollback 방식은 EC2 배포 스크립트에서 별도로 관리됩니다.

***중요 - 현재는 배포가 중단된 상태입니다. Github actions와 EC2, S3 모두 비활성화되었습니다. (update 2026.10.19)***

### S3 임시 영역 (pending/)

업로드는 `pending/{저장 파일명}`에 올라가고, 확정할 때 서버가 정식 키로 복사한 뒤 원본을 지웁니다. 등록되지 않은 업로드를 저장소가 직접 지우도록
버킷에 Lifecycle 규칙을 겁니다. 만료 기간(1일)은 업로드 토큰 TTL(기본 1시간)보다 길어야 합니다.

`lifecycle.json`:

```json
{
  "Rules": [
    {
      "ID": "expire-pending-uploads",
      "Filter": { "Prefix": "pending/" },
      "Status": "Enabled",
      "Expiration": { "Days": 1 }
    }
  ]
}
```

```powershell
aws s3api put-bucket-lifecycle-configuration --bucket <버킷> --lifecycle-configuration file://lifecycle.json
```

버킷에 이미 다른 Lifecycle 규칙이 있으면 이 명령이 전체를 덮어쓰므로, 기존 규칙을 조회(`get-bucket-lifecycle-configuration`)해 함께 넣습니다.

Lifecycle 규칙은 안전망입니다. 규칙이 없어도 고아 정리의 만료 PENDING 단계가 같은 파일을 지웁니다. 복사를 위해 애플리케이션의 AWS 자격 증명에는 같은
버킷 객체에 대한 `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` 권한이 필요합니다. `s3:ListBucket`은 저장소 스캔에 필요하고,
확정 단계도 이 권한에 의존합니다. 이 권한이 없으면 S3가 없는 키에 `404` 대신 `403`을 돌려줘 모든 확정이 실패합니다.

## 수동 스키마 변경

`prod`는 `ddl-auto: none`이므로 엔티티 스키마를 바꾸는 배포마다 SQL을 직접 적용해야 합니다. 변경 한 건당 [migrations](migrations/)에 파일 하나를
두고, 각 파일에 prod 적용 기록을 남깁니다. 아래 순서대로 적용합니다.

| 날짜 | 변경 | 문서 |
| --- | --- | --- |
| 2026-10-24 | 게시글·댓글 `version` 컬럼 | [2026-10-24-adoption-version-columns](migrations/2026-10-24-adoption-version-columns.md) |
| 2026-10-25 | `storage_file` 테이블 도입과 파일 테이블 전환 | [2026-10-25-storage-file](migrations/2026-10-25-storage-file.md) |

## 관측

애플리케이션은 다음 Actuator endpoint를 노출합니다.

- `/actuator/health`: DB와 Redis 연결 상태를 포함합니다.
- `/actuator/metrics`
- `/actuator/prometheus`

애플리케이션이 Actuator 접근을 제한하지 않으므로 운영에서는 reverse proxy나 보안 그룹으로 외부 접근을 통제해야 합니다.

부하 테스트 시나리오, 테스트 데이터 준비와 로컬 Prometheus/Grafana 사용법은 [부하 테스트와 관측](load-testing.md)을 참고하세요. 해당 관측 구성은 부하 테스트용이며 운영 모니터링 배포를 대신하지 않습니다.
