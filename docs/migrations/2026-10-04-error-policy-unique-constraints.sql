-- MySQL 8 / 실행 전 백업 및 쓰기 중단 필요. 대상 DB를 USE로 선택한 후 실행한다.
-- 기존 자동 생성 unique index를 컬럼 목록으로 찾아 이름을 통일한다.
-- 같은 컬럼의 중복 unique index는 표준 이름의 index를 확보한 후 제거한다.
DELIMITER $$
CREATE PROCEDURE rename_error_policy_unique(IN table_name_arg VARCHAR(64), IN columns_arg VARCHAR(255), IN target_name VARCHAR(64))
BEGIN
  DECLARE old_name VARCHAR(64);
  DECLARE target_count INT;
  SELECT COUNT(*) INTO target_count FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = table_name_arg AND index_name = target_name;
  SELECT MIN(index_name) INTO old_name FROM (
    SELECT index_name FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = table_name_arg
      AND non_unique = 0 AND index_name <> 'PRIMARY' AND index_name <> target_name
    GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = columns_arg
  ) matches;
  IF target_count = 0 THEN
    IF old_name IS NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Expected unique index missing; inspect schema before retrying';
    END IF;
    SET @ddl = CONCAT('ALTER TABLE `', table_name_arg, '` RENAME INDEX `', old_name, '` TO `', target_name, '`');
    PREPARE statement FROM @ddl;
    EXECUTE statement;
    DEALLOCATE PREPARE statement;
  END IF;
  duplicate_indexes: LOOP
    SELECT MIN(index_name) INTO old_name FROM (
      SELECT index_name FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = table_name_arg
        AND non_unique = 0 AND index_name <> 'PRIMARY' AND index_name <> target_name
      GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = columns_arg
    ) matches;
    IF old_name IS NULL THEN LEAVE duplicate_indexes; END IF;
    SET @ddl = CONCAT('ALTER TABLE `', table_name_arg, '` DROP INDEX `', old_name, '`');
    PREPARE statement FROM @ddl;
    EXECUTE statement;
    DEALLOCATE PREPARE statement;
  END LOOP;
END$$
DELIMITER ;
CALL rename_error_policy_unique('users', 'email', 'uk_users_email');
CALL rename_error_policy_unique('users', 'nickname', 'uk_users_nickname');
CALL rename_error_policy_unique('users', 'provider_info,provider_id', 'uk_users_provider');
CALL rename_error_policy_unique('adoption_adopters', 'adoption_post_id', 'uk_adoption_adopter_post');
CALL rename_error_policy_unique('user_image', 'user_id', 'uk_user_image_user');
CALL rename_error_policy_unique('room_participants', 'chat_room_id,user_id', 'uk_room_participant_room_user');
DROP PROCEDURE rename_error_policy_unique;
