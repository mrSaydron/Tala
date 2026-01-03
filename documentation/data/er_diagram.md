```mermaid

erDiagram
    words {
        INT id PK
        STRING word
        STRING translation
        STRING part_of_speech
        STRING ipa
        STRING hint
        STRING image_path
        INT base_word_id FK
        DOUBLE frequency
        STRING level
        STRING tags
    }
    word_collection {
        INT id PK
        STRING name
        STRING description
    }
    word_collection_entries {
        INT collection_id PK, FK
        INT word_id PK, FK
    }
    lessons {
        INT id PK
        STRING name
        STRING full_name
        INT collection_id FK
    }
    lesson_card_types {
        INT collection_id PK, FK
        STRING card_type PK
    }
    lesson_progress {
        INT id PK
        INT lesson_id FK
        STRING card_type
        INT word_id FK
        LONG next_review_date
        LONG interval_minutes
        DOUBLE ef
        STRING status
        STRING info
    }
    card_history {
        INT id PK
        INT lesson_id FK
        STRING card_type
        INT word_id FK
        INT quality
        LONG date
    }

    words ||--o{ words : "base_word_id (самоссылка)"
    words ||--o{ word_collection_entries : "слова в коллекциях"
    word_collection ||--o{ word_collection_entries : "подборки → слова"
    word_collection ||--o{ lessons : "уроки"
    word_collection ||--o{ lesson_card_types : "доступные типы карточек"
    lessons ||--o{ lesson_progress : "прогресс по уроку"
    words ||--o{ lesson_progress : "словарная привязка (nullable)"
    lessons ||--o{ card_history : "история ответов по уроку"
    words ||--o{ card_history : "словарная привязка к истории"
    ```