-- 기존 ID 값은 유지하고, 각 테이블의 현재 최댓값 다음부터 자동 증가시킨다.
ALTER TABLE `user_image`
    MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `adoption_post_image`
    MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `adoption_comment_image`
    MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `room_participants`
    MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE `chat_message_attachments`
    MODIFY COLUMN `id` BIGINT NOT NULL AUTO_INCREMENT;
