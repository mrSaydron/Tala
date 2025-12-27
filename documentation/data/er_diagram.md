```mermaid

erDiagram
    dictionary {
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
    dictionary_collections {
        INT id PK
        STRING name
        STRING description
    }
    dictionary_collection_entries {
        INT collection_id PK, FK
        INT dictionary_id PK, FK
    }
    card {
        INT id PK
        STRING common_id
        LONG next_review_date
        INT collection_id
        STRING info
        STRING card_type
        INT interval_minutes
        DOUBLE ef
        STRING status
    }
    collections {
        INT id PK
        STRING name
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
        INT dictionary_id FK
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
        INT dictionary_id FK
        INT quality
        LONG date
    }

    dictionary ||--o{ dictionary : "base_word_id (самоссылка)"
    dictionary ||--o{ dictionary_collection_entries : "слова в коллекциях"
    dictionary_collections ||--o{ dictionary_collection_entries : "подборки → слова"
    dictionary_collections ||--o{ lessons : "уроки"
    dictionary_collections ||--o{ lesson_card_types : "доступные типы карточек"
    lessons ||--o{ lesson_progress : "прогресс по уроку"
    dictionary ||--o{ lesson_progress : "словарная привязка (nullable)"
    collections ||--o{ card : "карточки коллекции"
    collections ||--o{ card : "карточки коллекции"
    lessons ||--o{ card_history : "история ответов по уроку"
    dictionary ||--o{ card_history : "словарная привязка к истории"
    ```