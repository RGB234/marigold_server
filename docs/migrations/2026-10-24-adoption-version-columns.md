# 2026-10-24 게시글·댓글 version 컬럼 추가

- 관련 커밋: `3d054e3 feat(adoption)!: 게시글·댓글 동시 수정에 낙관적 락 적용`
- prod 적용 기록: 없음. 적용하면 날짜를 여기에 적습니다.

`AdoptionPost`와 `AdoptionComment`가 `@Version`으로 낙관적 락을 사용하면서 두 테이블에 `version` 컬럼이 필요해졌습니다. `prod`는
`ddl-auto: none`이므로 배포 전에 직접 적용합니다. `local`은 `ddl-auto: update`가 컬럼을 추가합니다.

```sql
ALTER TABLE adoption_post ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE adoption_comment ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```
