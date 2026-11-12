# 2026-11-12 users.status에서 SLEEP 제거

- 관련 커밋: 없음. 커밋하면 여기에 적습니다.
- prod 적용 기록: 없음. 적용하면 날짜를 여기에 적습니다.

쓰는 곳이 없던 휴면 상태를 `UserStatus`에서 지웠습니다. `users.status`는 MySQL `enum` 컬럼이라 정의에 `SLEEP`이 남아 있어도 동작하지만,
`status = 'SLEEP'`인 행이 있으면 그 사용자를 읽을 때 실패합니다. 배포 전에 먼저 확인합니다.

```sql
-- 0이어야 합니다. 0이 아니면 그 사용자를 어떤 상태로 둘지 정해 먼저 바꿉니다
SELECT COUNT(*) FROM users WHERE status = 'SLEEP';

ALTER TABLE users MODIFY COLUMN status ENUM('ACTIVE', 'BANNED', 'DELETED') NOT NULL;
```

`local`의 `ddl-auto: update`는 기존 `enum` 컬럼 정의를 바꾸지 않습니다. 남아 있어도 문제는 없고, 맞추려면 같은 SQL을 적용합니다.
