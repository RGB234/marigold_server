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

기본값은 사용자 50명, 채팅방 50개, adoption post 5000개, post당 adoption comment 3개입니다. 변경하려면:

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

## 프로파일

- `tests/smoke.js`: 스크립트, 인증 데이터, WebSocket 연결 검증용
- `tests/load.js`: 예상 정상 부하를 45분 동안 검증
- `tests/stress.js`: 단계적으로 VU를 올려 한계점을 확인
- `tests/spike.js`: 순간 급증 후 회복 여부 확인

## 결과

각 테스트는 `results/{profile}-summary.json`에 요약 데이터를 저장합니다.

우선 확인할 지표:

- `http_req_failed`
- `http_req_duration p(95), p(99)`
- `checks`
- WebSocket `101` 성공률
- 서버 CPU, 메모리, JVM GC, DB connection pool, slow query

## 기준값

초기 기준입니다. 실제 SLO가 정해지면 조정합니다.

- Load: `http_req_failed < 1%`, `p95 < 500ms`, `p99 < 2s`
- Stress/Spike: `http_req_failed < 5%`, `p95 < 2s`, `p99 < 5s`
