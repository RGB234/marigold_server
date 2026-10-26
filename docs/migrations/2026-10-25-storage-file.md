# 2026-10-25 storage_file 테이블 도입

- 관련 커밋: `10ad299 feat(storage)!: 업로드 파일을 storage_file 테이블과 pending/ 임시 영역으로 관리`
- 함께 할 일: 운영 S3 버킷에 `pending/` Lifecycle 규칙을 추가합니다. 절차는 [실행과 운영](../operations.md#s3-임시-영역-pending)에 있습니다.
- 이전 상태에 따른 차이: `c5ecbf9`의 `uk_*_stored_file` 제약을 적용했는지에 따라 아래 SQL이 달라집니다.
- prod 적용 기록: 없음. 적용하면 날짜를 여기에 적습니다.

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
