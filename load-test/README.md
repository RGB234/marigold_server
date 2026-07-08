# Marigold Load Test

k6 기반 백엔드 부하 테스트입니다. 작업 디렉터리는 `back/load-test`로 두고 실행합니다.

## 준비

1. 백엔드를 local 또는 staging 환경에서 실행합니다.
2. 테스트용 사용자, 입양글, 채팅방을 미리 생성합니다.
3. `data/users.csv`, `data/rooms.json`을 대상 DB 데이터에 맞게 수정합니다.

운영 환경에 실행할 때는 별도 승인과 모니터링 준비 없이 실행하지 않습니다.

## 데이터 파일

`data/users.csv`

```csv
id,email,password
1,user1@example.com,password
2,user2@example.com,password
```

`data/rooms.json`

```json
[
  { "id": 1 },
  { "id": 2 }
]
```

실제 비밀번호를 저장소에 커밋하지 마세요. 필요하면 아래 환경 변수로 별도 파일을 지정합니다.

```powershell
$env:LOAD_TEST_USERS_FILE="./data/local-users.csv"
$env:LOAD_TEST_ROOMS_FILE="./data/local-rooms.json"
```

## 실행

PowerShell 기준:

```powershell
cd D:\ark\dev\projects\marigold\back\load-test
$env:BASE_URL="http://localhost:8080"
$env:WS_BASE_URL="ws://localhost:8080/ws/websocket"
```

Smoke test:

```powershell
k6 run tests/smoke.js
```

Load test:

```powershell
k6 run tests/load.js
```

Stress test:

```powershell
k6 run tests/stress.js
```

Spike test:

```powershell
k6 run tests/spike.js
```

기존 짧은 load profile은 아래 명령으로도 실행할 수 있습니다.

```powershell
k6 run main.js
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

