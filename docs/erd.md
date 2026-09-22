# ERD

이 문서는 현재 `src/main/java/com/sns/marigold` 아래 JPA `@Entity` 기준으로 작성했습니다.

실제 운영 DB 스키마와 동기화를 보장하지 않고 있으므로, 실제와 다소 차이가 있을 수 있습니다.

## Mermaid

```mermaid
erDiagram
    USERS {
        BIGINT id PK "TSID"
        VARCHAR provider_info "ProviderInfo, nullable"
        VARCHAR provider_id "nullable"
        VARCHAR email UK "nullable"
        VARCHAR password "nullable"
        VARCHAR role "Role, not null"
        VARCHAR nickname UK "length 50, not null"
        VARCHAR status "UserStatus, not null"
        DATETIME deleted_at "nullable"
    }

    USER_IMAGE {
        BIGINT id PK "auto increment"
        BIGINT user_id FK "unique, not null"
        DATETIME created_at "not null"
        VARCHAR stored_file_name "not null"
        VARCHAR original_file_name "not null"
    }

    ADOPTION_POST {
        BIGINT id PK "auto increment"
        DATETIME created_at "not null"
        DATETIME modified_at "not null"
        DATETIME deleted_at "nullable"
        BIGINT writer_id FK "not null"
        VARCHAR species "Species, required when active"
        VARCHAR sex "Sex, required when active"
        VARCHAR neutering "Neutering, required when active"
        VARCHAR area "not null"
        VARCHAR title "not null"
        TEXT features "not null"
        INTEGER age "nullable"
        DOUBLE weight "nullable"
        VARCHAR status "AdoptionPostStatus, not null"
    }

    ADOPTION_POST_IMAGE {
        BIGINT id PK "auto increment"
        DATETIME created_at "not null"
        VARCHAR stored_file_name "not null"
        VARCHAR original_file_name "not null"
        BIGINT adoption_post_id FK "not null"
    }

    ADOPTION_COMMENT {
        BIGINT id PK "auto increment"
        BIGINT adoption_post_id FK "not null"
        BIGINT writer_id FK "not null"
        BIGINT parent_id FK "nullable"
        TEXT content "not null"
        DATETIME created_at "not null"
        DATETIME modified_at "not null"
        DATETIME deleted_at "nullable"
    }

    ADOPTION_COMMENT_IMAGE {
        BIGINT id PK "auto increment"
        DATETIME created_at "not null"
        VARCHAR stored_file_name "not null"
        VARCHAR original_file_name "not null"
        BIGINT adoption_comment_id FK "not null"
    }

    ADOPTION_ADOPTERS {
        BIGINT id PK "auto increment"
        BIGINT adoption_post_id FK "unique, not null"
        BIGINT adopter_id FK "not null"
        DATETIME created_at "not null"
    }

    CHAT_ROOMS {
        BIGINT id PK "TSID"
        BIGINT adoption_post_id FK "not null"
        DATETIME created_at "not null"
        VARCHAR status "ChatRoomStatus, not null"
    }

    ROOM_PARTICIPANTS {
        BIGINT id PK "auto increment"
        BIGINT chat_room_id FK "not null"
        BIGINT user_id FK "not null"
        DATETIME joined_at "not null"
        DATETIME leaved_at "nullable"
    }

    CHAT_MESSAGES {
        BIGINT id PK "TSID"
        BIGINT chat_room_id FK "not null"
        BIGINT sender_id FK "not null"
        TEXT message "not null"
        DATETIME created_at "not null"
    }

    CHAT_MESSAGE_ATTACHMENTS {
        BIGINT id PK "auto increment"
        BIGINT chat_message_id FK "not null"
        VARCHAR stored_file_name "not null"
        VARCHAR original_file_name "not null"
        VARCHAR content_type "not null"
        BIGINT file_size "not null"
        DATETIME created_at "not null"
    }

    USER_IMAGE o|--|| USERS : "profile image"
    USERS ||--o{ ADOPTION_POST : "writes"
    ADOPTION_POST ||--o{ ADOPTION_POST_IMAGE : "has images"
    ADOPTION_POST ||--o{ ADOPTION_COMMENT : "has comments"
    USERS ||--o{ ADOPTION_COMMENT : "writes"
    ADOPTION_COMMENT o|--o{ ADOPTION_COMMENT : "has replies"
    ADOPTION_COMMENT ||--o{ ADOPTION_COMMENT_IMAGE : "has images"
    ADOPTION_POST ||--o| ADOPTION_ADOPTERS : "completed by"
    USERS ||--o{ ADOPTION_ADOPTERS : "adopts"
    ADOPTION_POST ||--o{ CHAT_ROOMS : "opens"
    CHAT_ROOMS ||--o{ ROOM_PARTICIPANTS : "has participants"
    USERS ||--o{ ROOM_PARTICIPANTS : "joins"
    CHAT_ROOMS ||--o{ CHAT_MESSAGES : "has messages"
    USERS ||--o{ CHAT_MESSAGES : "sends"
    CHAT_MESSAGES ||--o{ CHAT_MESSAGE_ATTACHMENTS : "has attachments"
```

## 제약 및 인덱스

| 테이블 | 제약/인덱스 | 내용 |
| --- | --- | --- |
| `users` | unique | `email`, `nickname`, `(provider_info, provider_id)` |
| `user_image` | unique | `user_id` |
| `adoption_post` | check | `deleted_at IS NOT NULL OR (species IS NOT NULL AND sex IS NOT NULL AND neutering IS NOT NULL)` |
| `adoption_adopters` | unique | `adoption_post_id` |
| `room_participants` | unique | `uk_room_participant_room_user(chat_room_id, user_id)` |
| `room_participants` | index | `idx_user(user_id)` |
