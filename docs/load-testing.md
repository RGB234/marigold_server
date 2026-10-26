# 부하 테스트와 관측

`load-test/`는 k6로 HTTP API와 WebSocket 부하를 발생시키고 Prometheus와 Grafana로 애플리케이션 및 k6 지표를 확인하기 위한 독립 실행 구성입니다. Gradle의 단위·컴포넌트·통합 테스트에는 포함되지 않습니다.

## 사전 요구사항

- k6
- Docker와 Docker Compose
- 테스트 데이터 생성·정리 시 MySQL client
- 부하 테스트 전용 데이터베이스와 테스트 계정

운영 데이터베이스에는 seed나 cleanup 스크립트를 실행하지 마세요. 두 스크립트의 `-ConfirmTestDatabase` 옵션은 사용자의 확인을 요구할 뿐 대상이 운영 환경인지 자동으로 판별하지 않습니다.

## 디렉터리 구성

| 경로 | 역할 |
| --- | --- |
| `run.ps1` | 환경파일을 읽고 선택한 k6 profile을 실행합니다. |
| `config.js` | 대상 URL, 부하량과 시드 데이터 범위를 환경변수에서 읽습니다. |
| `tests/` | smoke, load, stress, spike, DB와 저장소별 실행 profile입니다. |
| `scenarios/` | 인증, 입양 게시글, 채팅, DB와 저장소 요청을 구현합니다. |
| `thresholds.js` | 성공률, 실패율과 응답시간 통과 기준입니다. |
| `summary.js` | 콘솔 요약과 `results/*-summary.json` 생성을 담당합니다. |
| `db/` | 테스트 데이터 생성·삭제 SQL입니다. |
| `fixtures/storage/` | 파일 업로드 테스트용 1MB·5MB 이미지입니다. |
| `observability/` | Prometheus와 Grafana의 Docker Compose 구성입니다. |

## 실행 순서

이하 PowerShell 예시는 `back/load-test/` 디렉터리를 기준으로 합니다.

1. `.env.example`을 복사해 대상 환경파일을 준비합니다.
2. seed 스크립트로 테스트 데이터를 생성합니다.
3. Prometheus와 Grafana를 실행합니다.
4. 목적에 맞는 profile을 선택해 k6를 실행합니다.
5. 콘솔 요약, 결과 JSON과 Grafana dashboard를 확인합니다.
6. 테스트 데이터를 정리하고 관측 환경을 종료합니다.

## 1. 환경 설정

`.env.example`을 참고해 환경별 실제 값을 채운 `.env.local`과 `.env.staging` 파일을 준비합니다.

`.env.local`과 `.env.staging`은 Git에서 무시됩니다.

`run.ps1`, `seed-rds.ps1`, `cleanup-rds.ps1`은 모두 `-EnvFile`을 지정해야 합니다. 생략하거나 빈 값을 전달하거나 파일이 없으면 입력 요청 없이 즉시 오류로 종료합니다.

## 2. 테스트 데이터 준비

`seed-rds.ps1`은 기존 부하 테스트 데이터를 먼저 정리한 후 사용자, 입양 게시글, 이미지 메타데이터, 댓글, 채팅방과 참여자를 생성합니다.

```powershell
.\seed-rds.ps1 -EnvFile .env.local -ConfirmTestDatabase
```

seed가 완료되면 출력된 전체 페이지 수를 환경파일의 `LOAD_TEST_ADOPTION_TOTAL_PAGES`에 반영합니다. `LOAD_TEST_LOGIN_PASSWORD`는 seed에 사용한 `LOAD_TEST_PASSWORD_HASH`와 같은 비밀번호를 나타내야 합니다.

## 3. 관측 환경 준비

다음 명령으로 Prometheus와 Grafana를 실행합니다.

```powershell
Set-Location observability
docker compose up -d
Set-Location ..
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Grafana 기본 계정: `admin` / `admin`

Prometheus는 `host.docker.internal:8080/actuator/prometheus`에서 애플리케이션 지표를 5초마다 수집합니다.

이 대상은 로컬 백엔드 기준으로 고정되어 있으므로 원격 환경을 관측하려면 `observability/prometheus.yml`의 target을 별도로 조정해야 합니다.

## 4. 테스트 profile 선택

| Profile | 부하 구성 | 기본 실행시간 |
| --- | --- | --- |
| `smoke` | 입양·인증 각 1 VU, 채팅 1회 | 최대 80초 |
| `load` | 입양 75, 인증 5, 채팅 20 VU를 지속 | 17분 |
| `stress` | 총 400 VU까지 단계적으로 증가 | 40분 |
| `spike` | 총 20 VU에서 300 VU로 급증한 뒤 복구 | 20분 |
| `db-read` | 읽기 시나리오 초당 30회 | 5분 |
| `db-write` | 쓰기 시나리오 초당 5회 | 5분 |
| `db-mixed` | 초당 30회 중 읽기 80%, 쓰기 20% | 5분 |
| `storage-upload` | 이미지 게시글 생성과 선택적 삭제를 초당 2회 실행 | 5분 |
| `storage-mixed` | 이미지 게시글 생성·수정·삭제를 초당 2회 실행 | 5분 |

DB profile은 데이터베이스에 직접 쿼리하지 않고 애플리케이션 API를 호출해 DB 부하를 발생시킵니다. VU 비율은 동시 사용자 배분이며 실제 요청 비율과 같지 않습니다. 특히 채팅 시나리오는 기본 60초 동안 WebSocket 연결을 유지합니다.

저장소 profile은 게시글을 만들거나 수정할 때마다 [업로드 절차](contracts/uploads.md)대로 업로드 URL 발급(`*_upload_issue`), 파일 PUT(`*_upload_put`),
게시글 API 순서로 요청합니다. S3 대상에서는 파일 PUT이 애플리케이션을 거치지 않으므로, 애플리케이션 부하는 `*_upload_put`을 뺀 지표로 봅니다.

## 5. 실행

기본 smoke test는 다음과 같이 실행합니다.

```powershell
.\run.ps1 smoke -EnvFile .env.local
```

다른 profile은 첫 번째 인자로 지정합니다.

```powershell
.\run.ps1 load -EnvFile .env.local
.\run.ps1 stress -EnvFile .env.local
.\run.ps1 db-read -EnvFile .env.local
.\run.ps1 storage-mixed -EnvFile .env.local
```

스테이징 실행 시 `-EnvFile`에 해당 환경파일을 지정합니다.

```powershell
.\run.ps1 smoke -EnvFile .env.staging
```

k6 지표까지 Prometheus로 전송하려면 `K6_PROMETHEUS_RW_SERVER_URL`이 설정된 환경파일을 사용하고 다음 옵션을 추가합니다.

```powershell
.\run.ps1 smoke -EnvFile .env.local -PrometheusRemoteWrite
```

## 6. 통과 기준과 결과 확인

통과 기준은 `thresholds.js`에 정의되어 있습니다.

| Profile | Check 성공률 | HTTP 실패율 | 응답시간 |
| --- | --- | --- | --- |
| `smoke` | 95% 초과 | 5% 미만 | p95 1초 미만 |
| `load`, DB, 저장소 | 99% 초과 | 1% 미만 | p95 500ms, p99 2초 미만 |
| `stress`, `spike` | 95% 초과 | 5% 미만 | p95 2초 미만, p99 5초 미만 |

`summary.js`는 실행 종료 시 다음 형식의 요약을 콘솔에 출력합니다.

```text
test=load
http_req_failed.rate=...
http_req_duration.p95=...
http_req_duration.p99=...
checks.rate=...
adoption_list.p95=...
adoption_list.p99=...
...
```

- `test`: 실행한 profile 이름입니다.
- `http_req_failed.rate`: 전체 HTTP 요청 실패율입니다.
- `http_req_duration.p95`, `http_req_duration.p99`: 전체 HTTP 응답시간의 p95와 p99입니다.
- `checks.rate`: 전체 check 성공률입니다.
- `{metric}.p95`, `{metric}.p99`: 입양·인증·DB·저장소 endpoint와 WebSocket 연결의 세부 응답시간입니다.
- `n/a`: 해당 profile에서 그 지표를 생성하지 않았거나 집계값이 없는 경우입니다.

전체 k6 summary 데이터는 `results/{profile}-summary.json`에 저장됩니다. 같은 profile을 다시 실행하면 기존 파일을 덮어씁니다. 결과 JSON은 Git에서 무시되므로 필요한 결과는 별도로 보관합니다.

Grafana dashboard에서는 HTTP 응답시간·RPS·오류율, CPU, JVM heap·thread·GC, HikariCP, 인증 내부 지표와 k6 VU·network·WebSocket 지표를 확인할 수 있습니다. k6 지표는 `-PrometheusRemoteWrite` 옵션으로 실행했을 때 Prometheus에 전송됩니다.

## 7. 테스트 데이터와 관측 환경 정리

`cleanup-rds.ps1`은 지정한 환경파일을 먼저 읽은 후 DB 접속 정보와 정리 대상을 결정합니다.

```powershell
.\cleanup-rds.ps1 -EnvFile .env.local -ConfirmTestDatabase
```

정리는 `LOAD_TEST_SEED_EMAIL_PREFIX`와 `LOAD_TEST_SEED_EMAIL_DOMAIN`에 해당하는 사용자를 기준으로 연관된 채팅 메시지, 채팅방, 입양 게시글, 댓글과 이미지 메타데이터를 삭제합니다.

관측 환경을 종료할 때는 다음 명령을 사용합니다. Volume을 함께 삭제하지 않는 한 Prometheus와 Grafana 데이터는 유지됩니다.

```powershell
Set-Location observability
docker compose down
Set-Location ..
```

## 주의사항

- 부하 테스트 전 대상 URL과 데이터베이스가 테스트 전용 환경인지 확인합니다.
- `seed-rds.ps1`과 `cleanup-rds.ps1`은 운영 데이터베이스를 자동으로 차단하지 않습니다.
- 저장소 시나리오가 중간에 실패하면 생성한 게시글이나 파일이 남을 수 있습니다.
- 로컬 Prometheus/Grafana 구성은 부하 테스트 지원용이며 운영 모니터링 배포를 대신하지 않습니다.
- Actuator endpoint의 운영 노출과 접근 통제는 [실행과 운영](operations.md) `관측` 부분을 참고합니다.
