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
| `UPLOAD_TOKEN_SECRET` | 클라이언트 직접 업로드 토큰의 서명 키입니다. 32 byte 이상의 임의 문자열을 사용합니다. 비어 있으면 기동에 실패합니다. |


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

### 파일 테이블 마이그레이션 (storage_file)

`prod`는 `ddl-auto: none`이므로 직접 적용해야 합니다. `local`도 `ddl-auto: update`가 기존 NOT NULL 컬럼(`stored_file_name` 등)을 지우지
못해 저장이 실패하므로 같은 SQL을 실행하거나 개발 DB를 새로 만듭니다. 새 코드로 앱을 띄우기 전에 실행합니다. 먼저 띄웠다면 `update`가 만든
`storage_file` 테이블과 `storage_file_id` 컬럼을 지우고 실행합니다.

> [!WARNING]
> 고아 정리의 저장소 스캔은 `storage_file` 행이 없는 파일을 고아로 봅니다. `local`은 시작할 때 `dry-run=false`로 정리를 실행하므로, 백필 전에
> 새 코드로 앱을 띄우면 하루 넘게 지난 로컬 파일이 **모두 삭제됩니다**. 마이그레이션을 먼저 실행하고, 불안하면 첫 기동만 `dry-run: true`로
> 후보 로그를 확인합니다.

이 SQL은 다음을 합니다.

1. `storage_file`을 만듭니다.
2. 4개 파일 테이블의 행을 백필합니다. 상태는 `ATTACHED`이고, 소프트 삭제된 게시글의 이미지는 저장소에서 이미 지웠으므로 `DELETED`입니다.
   이미지 테이블에는 형식과 크기가 없어 `content_type`은 확장자로 정하고 `file_size`는 0으로 둡니다.
3. 각 테이블에 `storage_file_id`를 채운 뒤 NOT NULL로 바꾸고, `uk_*_stored_file`을 같은 이름으로 `storage_file_id`에 다시 만들고, FK를
   추가하고, 기존 파일 컬럼을 지웁니다.

`uk_*_stored_file`을 아직 적용하지 않은 DB라면 각 `DROP INDEX` 줄을 뺍니다. 제약 이름은 `UniqueConflictPolicy`가 `409 UPLOAD_ALREADY_USED`로
변환하는 기준이므로 바꾸지 않습니다.

```sql
-- 1. 파일 테이블
CREATE TABLE storage_file (
  id BIGINT NOT NULL AUTO_INCREMENT,
  stored_file_name VARCHAR(255) NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(255) NOT NULL,
  file_size BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  status_changed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_storage_file_name UNIQUE (stored_file_name),
  INDEX idx_storage_file_status_created (status, created_at)
);

-- 2. 기존 행 백필. 이미지 테이블에는 형식·크기가 없어 형식은 확장자로 정하고 크기는 0으로 둔다
INSERT INTO storage_file
  (stored_file_name, original_file_name, content_type, file_size, status, created_at, status_changed_at)
SELECT t.stored_file_name, t.original_file_name,
       CASE LOWER(SUBSTRING_INDEX(t.stored_file_name, '.', -1))
         WHEN 'jpg' THEN 'image/jpeg' WHEN 'jpeg' THEN 'image/jpeg'
         WHEN 'png' THEN 'image/png' WHEN 'webp' THEN 'image/webp'
         ELSE 'application/octet-stream' END,
       0, 'ATTACHED', t.created_at, t.created_at
FROM user_image t;

-- 소프트 삭제된 게시글의 이미지는 저장소에서 이미 지웠으므로 DELETED
INSERT INTO storage_file
  (stored_file_name, original_file_name, content_type, file_size, status, created_at, status_changed_at)
SELECT t.stored_file_name, t.original_file_name,
       CASE LOWER(SUBSTRING_INDEX(t.stored_file_name, '.', -1))
         WHEN 'jpg' THEN 'image/jpeg' WHEN 'jpeg' THEN 'image/jpeg'
         WHEN 'png' THEN 'image/png' WHEN 'webp' THEN 'image/webp'
         ELSE 'application/octet-stream' END,
       0, IF(p.deleted_at IS NULL, 'ATTACHED', 'DELETED'), t.created_at, t.created_at
FROM adoption_post_image t
JOIN adoption_post p ON p.id = t.adoption_post_id;

INSERT INTO storage_file
  (stored_file_name, original_file_name, content_type, file_size, status, created_at, status_changed_at)
SELECT t.stored_file_name, t.original_file_name,
       CASE LOWER(SUBSTRING_INDEX(t.stored_file_name, '.', -1))
         WHEN 'jpg' THEN 'image/jpeg' WHEN 'jpeg' THEN 'image/jpeg'
         WHEN 'png' THEN 'image/png' WHEN 'webp' THEN 'image/webp'
         ELSE 'application/octet-stream' END,
       0, 'ATTACHED', t.created_at, t.created_at
FROM adoption_comment_image t;

INSERT INTO storage_file
  (stored_file_name, original_file_name, content_type, file_size, status, created_at, status_changed_at)
SELECT t.stored_file_name, t.original_file_name, t.content_type, t.file_size,
       'ATTACHED', t.created_at, t.created_at
FROM chat_message_attachments t;

-- 3. 도메인 테이블을 storage_file_id 참조로 바꾼다 (테이블마다 같은 순서)
ALTER TABLE user_image ADD COLUMN storage_file_id BIGINT NULL;
UPDATE user_image t JOIN storage_file f ON f.stored_file_name = t.stored_file_name
SET t.storage_file_id = f.id;
ALTER TABLE user_image DROP INDEX uk_user_image_stored_file;
ALTER TABLE user_image
  MODIFY storage_file_id BIGINT NOT NULL,
  ADD CONSTRAINT uk_user_image_stored_file UNIQUE (storage_file_id),
  ADD CONSTRAINT fk_user_image_storage_file FOREIGN KEY (storage_file_id) REFERENCES storage_file (id),
  DROP COLUMN stored_file_name,
  DROP COLUMN original_file_name;

ALTER TABLE adoption_post_image ADD COLUMN storage_file_id BIGINT NULL;
UPDATE adoption_post_image t JOIN storage_file f ON f.stored_file_name = t.stored_file_name
SET t.storage_file_id = f.id;
ALTER TABLE adoption_post_image DROP INDEX uk_adoption_post_image_stored_file;
ALTER TABLE adoption_post_image
  MODIFY storage_file_id BIGINT NOT NULL,
  ADD CONSTRAINT uk_adoption_post_image_stored_file UNIQUE (storage_file_id),
  ADD CONSTRAINT fk_adoption_post_image_storage_file FOREIGN KEY (storage_file_id) REFERENCES storage_file (id),
  DROP COLUMN stored_file_name,
  DROP COLUMN original_file_name;

ALTER TABLE adoption_comment_image ADD COLUMN storage_file_id BIGINT NULL;
UPDATE adoption_comment_image t JOIN storage_file f ON f.stored_file_name = t.stored_file_name
SET t.storage_file_id = f.id;
ALTER TABLE adoption_comment_image DROP INDEX uk_adoption_comment_image_stored_file;
ALTER TABLE adoption_comment_image
  MODIFY storage_file_id BIGINT NOT NULL,
  ADD CONSTRAINT uk_adoption_comment_image_stored_file UNIQUE (storage_file_id),
  ADD CONSTRAINT fk_adoption_comment_image_storage_file FOREIGN KEY (storage_file_id) REFERENCES storage_file (id),
  DROP COLUMN stored_file_name,
  DROP COLUMN original_file_name;

ALTER TABLE chat_message_attachments ADD COLUMN storage_file_id BIGINT NULL;
UPDATE chat_message_attachments t JOIN storage_file f ON f.stored_file_name = t.stored_file_name
SET t.storage_file_id = f.id;
ALTER TABLE chat_message_attachments DROP INDEX uk_chat_message_attachments_stored_file;
ALTER TABLE chat_message_attachments
  MODIFY storage_file_id BIGINT NOT NULL,
  ADD CONSTRAINT uk_chat_message_attachments_stored_file UNIQUE (storage_file_id),
  ADD CONSTRAINT fk_chat_message_attachments_storage_file FOREIGN KEY (storage_file_id) REFERENCES storage_file (id),
  DROP COLUMN stored_file_name,
  DROP COLUMN original_file_name,
  DROP COLUMN content_type,
  DROP COLUMN file_size;
```

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
버킷 객체에 대한 `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` 권한이 필요하며, 저장소 스캔에는 `s3:ListBucket`이 필요합니다.


## 관측

애플리케이션은 다음 Actuator endpoint를 노출합니다.

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/prometheus`

애플리케이션이 Actuator 접근을 제한하지 않으므로 운영에서는 reverse proxy나 보안 그룹으로 외부 접근을 통제해야 합니다.

부하 테스트 시나리오, 테스트 데이터 준비와 로컬 Prometheus/Grafana 사용법은 [부하 테스트와 관측](load-testing.md)을 참고하세요. 해당 관측 구성은 부하 테스트용이며 운영 모니터링 배포를 대신하지 않습니다.
