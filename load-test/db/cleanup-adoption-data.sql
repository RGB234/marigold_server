SET @load_test_email_prefix := COALESCE(@load_test_email_prefix, 'loadtest-user-');
SET @load_test_email_domain := COALESCE(@load_test_email_domain, 'example.test');

DROP TEMPORARY TABLE IF EXISTS load_test_user_ids;
CREATE TEMPORARY TABLE load_test_user_ids (id BIGINT PRIMARY KEY);

INSERT INTO load_test_user_ids (id)
SELECT id
FROM users
WHERE email LIKE CONCAT(@load_test_email_prefix, '%@', @load_test_email_domain);

DROP TEMPORARY TABLE IF EXISTS load_test_post_ids;
CREATE TEMPORARY TABLE load_test_post_ids (id BIGINT PRIMARY KEY);

INSERT INTO load_test_post_ids (id)
SELECT id
FROM adoption_post
WHERE writer_id IN (SELECT id FROM load_test_user_ids);

DROP TEMPORARY TABLE IF EXISTS load_test_room_ids;
CREATE TEMPORARY TABLE load_test_room_ids (id BIGINT PRIMARY KEY);

INSERT IGNORE INTO load_test_room_ids (id)
SELECT id
FROM chat_rooms
WHERE adoption_post_id IN (SELECT id FROM load_test_post_ids);

INSERT IGNORE INTO load_test_room_ids (id)
SELECT chat_room_id
FROM room_participants
WHERE user_id IN (SELECT id FROM load_test_user_ids);

DROP TEMPORARY TABLE IF EXISTS load_test_message_ids;
CREATE TEMPORARY TABLE load_test_message_ids (id BIGINT PRIMARY KEY);

INSERT IGNORE INTO load_test_message_ids (id)
SELECT id
FROM chat_messages
WHERE chat_room_id IN (SELECT id FROM load_test_room_ids)
   OR sender_id IN (SELECT id FROM load_test_user_ids);

DELETE cma
FROM chat_message_attachments cma
JOIN load_test_message_ids m ON cma.chat_message_id = m.id;

DELETE cm
FROM chat_messages cm
JOIN load_test_message_ids m ON cm.id = m.id;

DELETE rp
FROM room_participants rp
WHERE EXISTS (
  SELECT 1
  FROM load_test_room_ids r
  WHERE r.id = rp.chat_room_id
)
OR EXISTS (
  SELECT 1
  FROM load_test_user_ids u
  WHERE u.id = rp.user_id
);

DELETE cr
FROM chat_rooms cr
JOIN load_test_room_ids r ON cr.id = r.id;

DELETE aa
FROM adoption_adopters aa
JOIN load_test_post_ids p ON aa.adoption_post_id = p.id;

DELETE aci
FROM adoption_comment_image aci
JOIN adoption_comment ac ON aci.adoption_comment_id = ac.id
JOIN load_test_post_ids p ON ac.adoption_post_id = p.id;

UPDATE adoption_comment ac
JOIN load_test_post_ids p ON ac.adoption_post_id = p.id
SET ac.parent_id = NULL;

DELETE ac
FROM adoption_comment ac
JOIN load_test_post_ids p ON ac.adoption_post_id = p.id;

DELETE api
FROM adoption_post_image api
JOIN load_test_post_ids p ON api.adoption_post_id = p.id;

DELETE ap
FROM adoption_post ap
JOIN load_test_post_ids p ON ap.id = p.id;

DELETE u
FROM users u
JOIN load_test_user_ids ids ON u.id = ids.id;

DROP TEMPORARY TABLE IF EXISTS load_test_message_ids;
DROP TEMPORARY TABLE IF EXISTS load_test_room_ids;
DROP TEMPORARY TABLE IF EXISTS load_test_post_ids;
DROP TEMPORARY TABLE IF EXISTS load_test_user_ids;
