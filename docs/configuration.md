# 설정과 환경변수

## 기준 파일

| 파일 | 책임 |
| --- | --- |
| [application.yml](../src/main/resources/application.yml) | 공통 기본값, OAuth 프로필 포함 |
| [application-local.yml](../src/main/resources/application-local.yml) | 로컬 DB·파일 저장소, 로컬 비밀파일 로딩 |
| [application-prod.yml](../src/main/resources/application-prod.yml) | 운영 DB·S3·로그·Swagger 제한 |
| [application-oauth.yml](../src/main/resources/application-oauth.yml) | OAuth 공급자 설정 |
| [.env.example](../.env.example) | 변수 목록, 용도별 구분과 안전한 예제 |

기본값은 YAML이 기준입니다. 예제의 선택 항목은 생략할 수 있지만 빈 값으로 덮어쓰지 않습니다. 설정 추가 시 YAML, 예제, 이 문서의 환경 구분을 함께 검토합니다.

## 로컬 실행

Java 17과 MySQL을 준비합니다. `back`에서 실행할 때 로컬 비밀파일은 `../secrets/back/.env.local`입니다. 예제를 해당 위치에 복사하고 JWT 키, OAuth 자격증명, DB 접속 정보를 채웁니다. 기존 파일은 덮어쓰지 않습니다. UTF-8로 저장합니다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

IDE에서도 활성 프로필을 `local`, 작업 디렉터리를 `back`으로 지정합니다. 상위 작업 디렉터리에서 실행하는 경우에는 `secrets/back/.env.local`을 읽습니다. 두 후보 파일이 모두 있으면 뒤에 선언된 `../secrets/back/.env.local`이 우선하므로 한 곳만 사용합니다.

루트 `.env`는 자동으로 읽지 않습니다. 현재 import 파일은 Java properties 형식으로 읽으므로 `KEY=value`를 사용하고 shell의 `export`나 값을 감싸는 따옴표를 넣지 않습니다. 경로에는 `/`를 권장합니다. 프로세스 환경변수는 파일 설정보다 우선하며, 실행 인자로 지정한 Spring 속성은 그보다 우선합니다.

## 환경별 필수 항목

| 구분 | 항목 |
| --- | --- |
| 공통 필수 | `JWT_SECRET_KEY`, `BASE_URL`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET` |
| 로컬 | `DB_*`, `FRONTEND_ORIGIN`, `LOCAL_STORAGE_*`는 YAML 기본값이 있으며 실제 로컬 환경에 맞게 지정 |
| 운영 필수 | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `FRONTEND_ORIGIN`, `AWS_S3_BUCKET`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_REGION` |
| 공통 선택 | `JWT_ACCESS_TOKEN_VALIDITY_SECONDS`, `JWT_REFRESH_TOKEN_VALIDITY_SECONDS` |

OAuth 프로필은 공통으로 포함되므로 로컬에서도 공급자 설정이 필요합니다. `BASE_URL`은 백엔드 외부 주소, `FRONTEND_ORIGIN`은 프론트 주소입니다. 운영에서는 예제의 localhost 값을 교체합니다.

## 운영과 테스트

운영은 `prod` 프로필을 명시합니다. `/home/ec2-user/app.env`를 선택적으로 import하지만, 컨테이너에서는 해당 파일을 마운트하거나 `--env-file`로 프로세스 환경변수를 전달해야 합니다. 호스트 파일이 자동으로 컨테이너에 전달되지는 않습니다. 상세 절차는 [배포](deployment.md)를 봅니다.

[테스트 설정](../src/test/resources/application-test.yaml)은 테스트 전용입니다. 부하 테스트 환경파일은 [부하 테스트 안내](load-test.md)에서 관리하며 애플리케이션 실행용 환경파일과 구분합니다.
