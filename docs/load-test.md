# Marigold Load Test

k6 기반 백엔드 부하 테스트입니다. 작업 디렉터리는 `back/load-test`로 두고 실행합니다.

## 준비

1. 백엔드를 local 또는 staging 환경에서 실행합니다.
2. staging 백엔드는 테스트 전용 DB 또는 테스트 전용 RDS를 바라보게 합니다.
3. `seed-rds.ps1`로 테스트용 사용자, 입양글, 채팅방을 생성합니다.

운영 환경에 실행할 때는 별도 승인과 모니터링 준비 없이 실행하지 않습니다.

## Staging/RDS 구성

권장 흐름:

1. 운영과 같은 MySQL engine/version/schema/index를 가진 테스트 전용 RDS를 준비합니다.
2. staging 백엔드가 해당 RDS를 바라보게 배포합니다.
3. `seed-rds.ps1`로 테스트 사용자, 채팅방, adoption post, adoption comment를 생성합니다.
4. `run.ps1`로 smoke/load/stress/spike를 실행합니다.
5. 필요하면 `cleanup-rds.ps1`로 테스트 데이터를 제거합니다.

추후 AWS 내부에서 더 현실적으로 테스트할 때도 같은 파일을 사용합니다.

- staging backend: 같은 VPC의 EC2/ECS/Elastic Beanstalk 등
- RDS: private subnet
- k6 runner: 같은 VPC의 EC2/ECS task/self-hosted runner
- 보안그룹: k6 runner -> staging backend, staging backend -> RDS만 허용

이 구조에서는 `BASE_URL`, `WS_BASE_URL`, DB 접속 환경변수만 바꾸면 됩니다.

## Seed/Cleanup

`seed-rds.ps1`은 MySQL CLI를 사용합니다. RDS가 private subnet이면 같은 VPC 안의 runner에서 실행합니다.
실행 시 `cleanup-adoption-data.sql`로 기존 load-test 데이터를 먼저 삭제한 뒤 `seed-adoption-data.sql`로 새 데이터를 생성합니다.
삭제 직전에 최종 호스트, 포트, DB 이름을 출력합니다. `-ConfirmTestDatabase`는 해당 대상이 테스트 전용임을 확인한 뒤 지정합니다.

설정 우선순위는 명시한 스크립트 인자 → 환경변수 → 스크립트 기본값입니다. 현재 작업 디렉터리의 `.env`를 먼저 읽으며, 파일의 값은 같은 이름의 기존 환경변수를 덮어씁니다. 다른 파일은 `-EnvFile .env.staging`으로 지정합니다. 기본 `.env`는 없어도 되지만, 명시한 환경파일이 없으면 오류로 종료합니다.

비밀번호는 DB에는 bcrypt hash로 들어가고, k6 로그인에는 `LOAD_TEST_LOGIN_PASSWORD` 원문 비밀번호를 사용합니다. 두 값은 반드시 같은 비밀번호여야 합니다.

```powershell
cd D:\ark\dev\projects\marigold\back\load-test

$env:LOAD_TEST_DB_HOST="marigold-load-test.xxxxxxxxxxxx.ap-northeast-2.rds.amazonaws.com"
$env:LOAD_TEST_DB_NAME="marigold_load_test"
$env:LOAD_TEST_DB_USERNAME="load_test_admin"
$env:LOAD_TEST_DB_PASSWORD="..."
$env:LOAD_TEST_LOGIN_PASSWORD="..."
$env:LOAD_TEST_PASSWORD_HASH="..."

.\seed-rds.ps1 -ConfirmTestDatabase
```

환경변수와 인자가 없을 때의 기본값은 사용자 50명, 채팅방은 사용자 수와 동일, adoption post 5,000개, post당 adoption comment 3개입니다. `.env.example`을 `.env`로 복사하면 게시글 수는 예제에 지정된 500개가 적용됩니다. 변경하려면:

```powershell
.\seed-rds.ps1 -UserCount 100 -ChatRoomCount 100 -PostCount 20000 -CommentsPerPost 5 -ConfirmTestDatabase
```

생성 후 출력되는 `LOAD_TEST_ADOPTION_TOTAL_PAGES` 값을 `.env` 또는 `.env.staging`에 반영합니다.

cleanup:

```powershell
.\cleanup-rds.ps1 -ConfirmTestDatabase
```

cleanup은 `LOAD_TEST_SEED_EMAIL_PREFIX`와 `LOAD_TEST_SEED_EMAIL_DOMAIN`에 해당하는 테스트 사용자와 그 사용자가 작성한 adoption post, 연결된 댓글/이미지/채팅 데이터를 제거합니다. 테스트 전용 DB에서만 실행합니다.

## Seed 데이터 규칙

사용자 풀은 CSV가 아니라 `db/seed-adoption-data.sql`의 생성 규칙과 같은 환경 변수로 계산합니다.

```properties
LOAD_TEST_SEED_USER_COUNT=50
LOAD_TEST_SEED_USER_ID_BASE=990000000000000000
LOAD_TEST_SEED_EMAIL_PREFIX=loadtest-user-
LOAD_TEST_SEED_EMAIL_DOMAIN=example.test
LOAD_TEST_LOGIN_PASSWORD=...
```

채팅방 풀도 `db/seed-adoption-data.sql`의 생성 규칙과 같은 환경 변수로 계산합니다.

```properties
LOAD_TEST_SEED_CHAT_ROOM_COUNT=50
LOAD_TEST_SEED_CHAT_ROOM_ID_BASE=993000000000000000
LOAD_TEST_SEED_ROOM_PARTICIPANT_ID_BASE=994000000000000000
```

실제 비밀번호를 저장소에 커밋하지 마세요.

또는 `.env` 파일을 수정하고 `run.ps1`로 실행합니다. `.env`는 Git 추적 대상이 아니고, 기본값 예시는 `.env.example`에 있습니다.
staging 예시는 `.env.staging.example`에 있습니다.

k6 지표를 로컬 Prometheus에 실시간으로 보내려면 `.env` 또는 `.env.staging`에 remote write endpoint를 둡니다.

```properties
K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write
K6_PROMETHEUS_RW_TREND_STATS=p(90),p(95),p(99),avg,min,max
```

## Adoption 페이지 선택

기본 smoke 테스트는 0페이지를 고정 조회합니다. load/stress/spike에서는 아래 설정으로 실제 사용자 흐름에 가깝게 페이지를 분산할 수 있습니다.

```properties
LOAD_TEST_ADOPTION_PAGE_SIZE=10
LOAD_TEST_ADOPTION_TOTAL_PAGES=500
LOAD_TEST_ADOPTION_PAGE_SELECTION=weighted
```

지원 모드:

- `fixed`: `LOAD_TEST_ADOPTION_FIXED_PAGE`만 조회
- `random`: 전체 페이지에서 균등 랜덤
- `weighted`: 70%는 0~2페이지, 20%는 3~20페이지, 10%는 더 깊은 페이지 조회

## 채팅 세션

채팅 시나리오는 VU별 첫 iteration에서만 로그인하고, 이후 같은 VU에서는 발급받은 Access Token과 CSRF 토큰을 재사용합니다. 따라서 `auth_login`은 인증 시나리오의 로그인 부하, `chat_auth_login`은 채팅 VU bootstrap 로그인을 의미합니다.

WebSocket 연결 유지 시간은 아래 값으로 조정합니다.

```properties
LOAD_TEST_CHAT_SESSION_SECONDS=60
```

## 실행

PowerShell 기준:

```powershell
cd D:\ark\dev\projects\marigold\back\load-test
```

Smoke test:

```powershell
.\run.ps1 smoke
```

Load test:

```powershell
.\run.ps1 load
```

Stress test:

```powershell
.\run.ps1 stress
```

Spike test:

```powershell
.\run.ps1 spike
```

DB read I/O test:

```powershell
.\run.ps1 db-read
```

DB write I/O test:

```powershell
.\run.ps1 db-write
```

DB mixed I/O test:

```powershell
.\run.ps1 db-mixed
```

Storage upload I/O test:

```powershell
.\run.ps1 storage-upload
```

Storage mixed I/O test:

```powershell
.\run.ps1 storage-mixed
```

기존 짧은 load profile은 아래 명령으로도 실행할 수 있습니다.

```powershell
.\run.ps1 main
```

직접 환경변수를 지정해서 실행할 수도 있습니다.

```powershell
$env:BASE_URL="http://localhost:8080"
$env:WS_BASE_URL="ws://localhost:8080/ws/websocket"
$env:API_VERSION="/api/v1"
k6 run tests/smoke.js
```

staging 예시 파일을 쓰려면:

```powershell
Copy-Item .env.staging.example .env.staging
notepad .env.staging
.\run.ps1 smoke -EnvFile .env.staging
.\run.ps1 load -EnvFile .env.staging
```

k6의 `http_req_duration`, `http_req_failed`, `checks`, `vus` 같은 클라이언트 지표를 Grafana에서 실시간으로 보려면 Prometheus/Grafana를 띄운 뒤 remote write 옵션을 켭니다.

```powershell
.\run.ps1 smoke -EnvFile .env.staging -PrometheusRemoteWrite
.\run.ps1 load -EnvFile .env.staging -PrometheusRemoteWrite
```

## 로컬 관측 환경

백엔드는 `/actuator/prometheus`로 Micrometer 지표를 노출합니다. 로컬에서 Prometheus와 Grafana를 같이 띄우려면:

```powershell
cd D:\ark\dev\projects\marigold\back\load-test\observability
docker compose up -d
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin` / `admin`)
- Grafana dashboard: `Marigold Load Test`
- scrape 대상: `http://host.docker.internal:8080/actuator/prometheus`
- k6 remote write endpoint: `.env` 또는 `.env.staging`의 `K6_PROMETHEUS_RW_SERVER_URL`

공개 포트는 `127.0.0.1`에 바인딩되며, 이 구성은 로컬 개발용입니다. 기본 수집 대상은 Docker Desktop 호스트의 백엔드입니다. Linux Docker Engine에서는 `host.docker.internal`을 호스트 게이트웨이에 매핑하는 별도 설정이 필요합니다. staging을 관측하려면 `prometheus.yml`의 `scheme`과 `targets`를 해당 서버에 맞춰 변경합니다. k6의 `BASE_URL` 변경은 Prometheus 수집 대상에 반영되지 않습니다.

백엔드를 `localhost:8080`으로 실행한 뒤 k6 테스트를 돌리면 Grafana에서 `http_server_requests_seconds` 기준 endpoint별 처리 시간과 실패율을 볼 수 있습니다.

## 프로파일

- `tests/smoke.js`: 스크립트, 인증 데이터, WebSocket 연결 검증용
- `tests/load.js`: 17분의 부하 단계(증가 5분, 유지 10분, 감소 2분)로 예상 정상 부하를 검증. 진행 중인 요청의 종료 유예는 별도이며, 채팅 시나리오는 `gracefulStop: '100s'`로 설정
- `tests/stress.js`: 단계적으로 VU를 올려 한계점을 확인
- `tests/spike.js`: 순간 급증 후 회복 여부 확인
- `tests/db-read.js`: 입양글 목록/상세/댓글/작성자 조회로 DB read I/O 확인
- `tests/db-write.js`: 작성자 게시글 상태 변경 왕복으로 DB write I/O 확인
- `tests/db-mixed.js`: DB read/write를 8:2 비율로 동시에 확인
- `tests/storage-upload.js`: 이미지 포함 입양글 생성으로 storage upload I/O 확인
- `tests/storage-mixed.js`: 이미지 포함 입양글 생성/수정/삭제로 storage upload/delete I/O 확인

I/O profile은 `constant-arrival-rate`를 사용합니다. 기본 조절값:

```properties
LOAD_TEST_IO_DURATION=5m
LOAD_TEST_DB_READ_RATE=30
LOAD_TEST_DB_WRITE_RATE=5
LOAD_TEST_DB_MIXED_RATE=30
LOAD_TEST_STORAGE_UPLOAD_RATE=2
LOAD_TEST_STORAGE_MIXED_RATE=2
LOAD_TEST_STORAGE_IMAGE_COUNT=1
LOAD_TEST_STORAGE_IMAGE_PATH=../fixtures/storage/image-1mb.jpg
LOAD_TEST_STORAGE_DELETE_CREATED=true
```

Storage profile은 실제 S3 put/delete를 발생시킬 수 있습니다. 테스트 전용 bucket/prefix 또는 테스트 전용 계정에서만 실행하고, 비용과 객체 누적을 확인하세요.

## 결과

각 테스트는 `results/{profile}-summary.json`에 요약 데이터를 저장합니다.

우선 확인할 지표:

- `http_req_failed`
- `http_req_duration p(95), p(99)`
- `checks`
- WebSocket `101` 성공률
- `auth_login`과 `chat_auth_login`의 분리된 p95/p99
- 서버 CPU, 메모리, JVM GC, DB connection pool, slow query

## 기준값

초기 기준입니다. 실제 SLO가 정해지면 조정합니다.

- Load: `http_req_failed < 1%`, `p95 < 500ms`, `p99 < 2s`
- Stress/Spike: `http_req_failed < 5%`, `p95 < 2s`, `p99 < 5s`
