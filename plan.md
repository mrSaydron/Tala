## План: переименование dictionary → word

1) Переименовать layout-файлы  
- `app/src/main/res/layout/fragment_dictionary_list.xml` → `fragment_word_list.xml`  
- `app/src/main/res/layout/fragment_dictionary_add.xml` → `fragment_word_add.xml`  
- `app/src/main/res/layout/item_dictionary_entry.xml` → `item_word_entry.xml`  
- `app/src/main/res/layout/item_dictionary_edit.xml` → `item_word_edit.xml`  
- `app/src/main/res/layout/item_dictionary_group.xml` → `item_word_group.xml`  
- `app/src/main/res/layout/item_dictionary_group_add.xml` → `item_word_group_add.xml`  
- `app/src/main/res/layout/item_dictionary_group_header.xml` → `item_word_group_header.xml`  
- `app/src/main/res/layout/item_dictionary_group_word.xml` → `item_word_group_word.xml`

2) Обновить ID внутри layout  
- `@+id/dictionary…` → `@+id/word…` (только в этих файлах).

3) Обновить ссылки в коде/XML  
- Kotlin: `fragment/DictionaryListFragment.kt`, `fragment/DictionaryAddFragment.kt`, адаптеры `fragment/adapter/Dictionary*`, `LessonListFragment.kt`, сервисы/DTO, где используются ресурсы/ID.  
- XML: `tools:listitem`, nav/menu, прочие ссылки.

4) Проверка  
- Поиск по проекту на `dictionary` в именах ресурсов/ID/ссылках; убедиться, что остались только доменные термины (API и т.п. можно оставить).
