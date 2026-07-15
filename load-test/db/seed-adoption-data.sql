SET @load_test_user_count := COALESCE(@load_test_user_count, 50);
SET @load_test_post_count := COALESCE(@load_test_post_count, 5000);
SET @load_test_comments_per_post := COALESCE(@load_test_comments_per_post, 3);
SET @load_test_chat_room_count := COALESCE(@load_test_chat_room_count, @load_test_user_count);
SET @load_test_user_id_base := COALESCE(@load_test_user_id_base, 990000000000000000);
SET @load_test_post_id_base := COALESCE(@load_test_post_id_base, 991000000000000000);
SET @load_test_image_id_base := COALESCE(@load_test_image_id_base, 992000000000000000);
SET @load_test_chat_room_id_base := COALESCE(@load_test_chat_room_id_base, 993000000000000000);
SET @load_test_room_participant_id_base := COALESCE(@load_test_room_participant_id_base, 994000000000000000);
SET @load_test_email_prefix := COALESCE(@load_test_email_prefix, 'loadtest-user-');
SET @load_test_email_domain := COALESCE(@load_test_email_domain, 'example.test');
SET @load_test_nickname_prefix := COALESCE(@load_test_nickname_prefix, 'loadtest-user-');
SET @load_test_image_prefix := COALESCE(@load_test_image_prefix, 'loadtest/adoption');

DROP TEMPORARY TABLE IF EXISTS load_test_numbers;
CREATE TEMPORARY TABLE load_test_numbers (n INT PRIMARY KEY);

INSERT INTO load_test_numbers (n)
SELECT d0.i + d1.i * 10 + d2.i * 100 + d3.i * 1000 + d4.i * 10000 + 1 AS n
FROM (
  SELECT 0 AS i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) d0
CROSS JOIN (
  SELECT 0 AS i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) d1
CROSS JOIN (
  SELECT 0 AS i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) d2
CROSS JOIN (
  SELECT 0 AS i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) d3
CROSS JOIN (
  SELECT 0 AS i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) d4
WHERE d0.i + d1.i * 10 + d2.i * 100 + d3.i * 1000 + d4.i * 10000 + 1
      <= GREATEST(
        @load_test_user_count,
        @load_test_post_count,
        @load_test_comments_per_post,
        @load_test_chat_room_count
      );

DROP TEMPORARY TABLE IF EXISTS load_test_comment_numbers;
CREATE TEMPORARY TABLE load_test_comment_numbers (n INT PRIMARY KEY);

INSERT INTO load_test_comment_numbers (n)
SELECT n
FROM load_test_numbers
WHERE n <= @load_test_comments_per_post;

INSERT INTO users (
  id,
  provider_info,
  provider_id,
  email,
  password,
  role,
  nickname,
  image_id,
  status,
  deleted_at
)
SELECT
  @load_test_user_id_base + n,
  NULL,
  NULL,
  CONCAT(@load_test_email_prefix, LPAD(n, 6, '0'), '@', @load_test_email_domain),
  @load_test_password_hash,
  'ROLE_PERSON',
  CONCAT(@load_test_nickname_prefix, LPAD(n, 6, '0')),
  NULL,
  'ACTIVE',
  NULL
FROM load_test_numbers
WHERE n <= @load_test_user_count;

INSERT INTO adoption_post (
  id,
  created_at,
  modified_at,
  deleted_at,
  writer_id,
  species,
  sex,
  neutering,
  area,
  title,
  features,
  age,
  weight,
  status
)
SELECT
  @load_test_post_id_base + n,
  TIMESTAMPADD(SECOND, -n, NOW()),
  TIMESTAMPADD(SECOND, -n, NOW()),
  NULL,
  @load_test_user_id_base + 1 + MOD(n - 1, @load_test_user_count),
  CASE MOD(n, 7)
    WHEN 0 THEN 'DOG'
    WHEN 1 THEN 'CAT'
    WHEN 2 THEN 'RODENTS'
    WHEN 3 THEN 'BIRDS'
    WHEN 4 THEN 'REPTILES'
    WHEN 5 THEN 'FISH'
    ELSE 'OTHER'
  END,
  CASE MOD(n, 4)
    WHEN 0 THEN 'MALE'
    WHEN 1 THEN 'FEMALE'
    WHEN 2 THEN 'UNKNOWN'
    ELSE 'OTHER'
  END,
  CASE MOD(n, 3)
    WHEN 0 THEN 'YES'
    WHEN 1 THEN 'NO'
    ELSE 'UNKNOWN'
  END,
  CASE MOD(n, 5)
    WHEN 0 THEN 'Seoul'
    WHEN 1 THEN 'Gyeonggi'
    WHEN 2 THEN 'Incheon'
    WHEN 3 THEN 'Busan'
    ELSE 'Daegu'
  END,
  CONCAT('Load test ', n),
  CONCAT('Load test adoption post fixture number ', n, '. This row is generated for staging performance tests.'),
  MOD(n, 10),
  ROUND(1 + MOD(n, 250) / 10, 1),
  CASE MOD(n, 10)
    WHEN 0 THEN 'RESERVED'
    WHEN 1 THEN 'COMPLETED'
    ELSE 'PROCEEDING'
  END
FROM load_test_numbers
WHERE n <= @load_test_post_count;

INSERT INTO adoption_post_image (
  id,
  created_at,
  stored_file_name,
  original_file_name,
  adoption_post_id
)
SELECT
  @load_test_image_id_base + n,
  TIMESTAMPADD(SECOND, -n, NOW()),
  CONCAT(@load_test_image_prefix, '/', LPAD(n, 6, '0'), '.jpg'),
  CONCAT('loadtest-', LPAD(n, 6, '0'), '.jpg'),
  @load_test_post_id_base + n
FROM load_test_numbers
WHERE n <= @load_test_post_count;

INSERT INTO chat_rooms (
  id,
  adoption_post_id,
  created_at,
  status
)
SELECT
  @load_test_chat_room_id_base + n,
  @load_test_post_id_base + 1 + MOD(n - 1, @load_test_post_count),
  TIMESTAMPADD(SECOND, -n, NOW()),
  'ACTIVE'
FROM load_test_numbers
WHERE n <= @load_test_chat_room_count
  AND @load_test_user_count > 1;

INSERT INTO room_participants (
  id,
  chat_room_id,
  user_id,
  joined_at,
  leaved_at
)
SELECT
  @load_test_room_participant_id_base + n,
  @load_test_chat_room_id_base + n,
  @load_test_user_id_base + 1 + MOD(n - 1, @load_test_user_count),
  TIMESTAMPADD(SECOND, -n, NOW()),
  NULL
FROM load_test_numbers
WHERE n <= @load_test_chat_room_count
  AND @load_test_user_count > 1;

INSERT INTO room_participants (
  id,
  chat_room_id,
  user_id,
  joined_at,
  leaved_at
)
SELECT
  @load_test_room_participant_id_base + @load_test_chat_room_count + n,
  @load_test_chat_room_id_base + n,
  @load_test_user_id_base + 1 + MOD(n, @load_test_user_count),
  TIMESTAMPADD(SECOND, -n, NOW()),
  NULL
FROM load_test_numbers
WHERE n <= @load_test_chat_room_count
  AND @load_test_user_count > 1;

INSERT INTO adoption_comment (
  adoption_post_id,
  writer_id,
  parent_id,
  content,
  created_at,
  modified_at,
  deleted_at
)
SELECT
  @load_test_post_id_base + post_numbers.n,
  @load_test_user_id_base + 1 + MOD(post_numbers.n + comment_numbers.n - 1, @load_test_user_count),
  NULL,
  CONCAT(
    'Load test adoption comment ',
    comment_numbers.n,
    ' for adoption post ',
    post_numbers.n,
    '. This row is generated for staging performance tests.'
  ),
  TIMESTAMPADD(SECOND, -(post_numbers.n * 10 + comment_numbers.n), NOW()),
  TIMESTAMPADD(SECOND, -(post_numbers.n * 10 + comment_numbers.n), NOW()),
  NULL
FROM load_test_numbers post_numbers
CROSS JOIN load_test_comment_numbers comment_numbers
WHERE post_numbers.n <= @load_test_post_count;

DROP TEMPORARY TABLE IF EXISTS load_test_comment_numbers;
DROP TEMPORARY TABLE IF EXISTS load_test_numbers;
