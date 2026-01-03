package com.example.tala.service.lessonCard

import com.example.tala.entity.cardhistory.CardHistory
import com.example.tala.entity.cardhistory.CardHistoryDao
import com.example.tala.entity.cardhistory.CardHistoryRepository
import com.example.tala.entity.word.Word
import com.example.tala.entity.word.WordDao
import com.example.tala.entity.word.WordRepository
import com.example.tala.entity.word.WordWithDependentCount
import com.example.tala.entity.word.PartOfSpeech
import com.example.tala.entity.wordCollection.WordCollection
import com.example.tala.entity.wordCollection.WordCollectionDao
import com.example.tala.entity.wordCollection.WordCollectionEntry
import com.example.tala.entity.wordCollection.WordCollectionEntryDao
import com.example.tala.entity.wordCollection.WordCollectionRepository
import com.example.tala.entity.wordCollection.WordCollectionWithEntries
import com.example.tala.entity.lesson.Lesson
import com.example.tala.entity.lesson.LessonDao
import com.example.tala.entity.lesson.LessonRepository
import com.example.tala.entity.lessoncardtype.LessonCardType
import com.example.tala.entity.lessoncardtype.LessonCardTypeDao
import com.example.tala.entity.lessoncardtype.LessonCardTypeRepository
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.entity.lessonprogress.LessonProgressDao
import com.example.tala.entity.lessonprogress.LessonProgressRepository
import com.example.tala.model.dto.lessonCard.LessonCardDto
import com.example.tala.model.dto.lessonCard.TranslateLessonCardDto
import com.example.tala.model.dto.lessonCard.TranslationComparisonLessonCardDto
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import com.example.tala.service.lessonCard.model.CardAnswer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class LessonCardServiceTest {

    private lateinit var lessonDao: FakeLessonDao
    private lateinit var lessonCardTypeDao: FakeLessonCardTypeDao
    private lateinit var dictionaryCollectionDao: FakeWordCollectionDao
    private lateinit var dictionaryCollectionEntryDao: FakeWordCollectionEntryDao
    private lateinit var dictionaryDao: FakeWordDao
    private lateinit var lessonProgressDao: FakeLessonProgressDao
    private lateinit var cardHistoryDao: FakeCardHistoryDao

    private lateinit var lessonRepository: LessonRepository
    private lateinit var lessonCardTypeRepository: LessonCardTypeRepository
    private lateinit var dictionaryCollectionRepository: WordCollectionRepository
    private lateinit var dictionaryRepository: WordRepository
    private lateinit var lessonProgressRepository: LessonProgressRepository
    private lateinit var cardHistoryRepository: CardHistoryRepository

    private lateinit var translateService: TranslateLessonCardTypeService
    private lateinit var lessonCardService: LessonCardService

    @Before
    fun setup() {
        lessonDao = FakeLessonDao().apply {
            lessons[LESSON_ID] = Lesson(
                id = LESSON_ID,
                name = "Lesson $LESSON_ID",
                fullName = "Lesson Full $LESSON_ID",
                collectionId = COLLECTION_ID
            )
        }
        lessonRepository = LessonRepository(lessonDao)

        lessonCardTypeDao = FakeLessonCardTypeDao().apply {
            cardTypes[COLLECTION_ID] = mutableListOf(
                LessonCardType(collectionId = COLLECTION_ID, cardType = CardTypeEnum.TRANSLATE)
            )
        }
        lessonCardTypeRepository = LessonCardTypeRepository(lessonCardTypeDao)

        dictionaryCollectionDao = FakeWordCollectionDao()

        dictionaryCollectionEntryDao = FakeWordCollectionEntryDao().apply {
            entries[COLLECTION_ID] = mutableListOf(
                WordCollectionEntry(COLLECTION_ID, 1),
                WordCollectionEntry(COLLECTION_ID, 2),
                WordCollectionEntry(COLLECTION_ID, 3)
            )
        }
        dictionaryCollectionRepository = WordCollectionRepository(
            dictionaryCollectionDao,
            dictionaryCollectionEntryDao
        )

        dictionaryDao = FakeWordDao(
            mutableMapOf(
                1 to Word(
                    id = 1,
                    word = "base-one",
                    translation = "перевод один",
                    partOfSpeech = PartOfSpeech.NOUN,
                    baseWordId = null
                ),
                2 to Word(
                    id = 2,
                    word = "base-two",
                    translation = "перевод два",
                    partOfSpeech = PartOfSpeech.NOUN,
                    baseWordId = 2
                ),
                3 to Word(
                    id = 3,
                    word = "derived",
                    translation = "перевод производное",
                    partOfSpeech = PartOfSpeech.NOUN,
                    baseWordId = 1
                )
            )
        )
        dictionaryRepository = WordRepository(dictionaryDao)

        lessonProgressDao = FakeLessonProgressDao()
        lessonProgressRepository = LessonProgressRepository(lessonProgressDao)
        cardHistoryDao = FakeCardHistoryDao()
        cardHistoryRepository = CardHistoryRepository(cardHistoryDao)

        translateService = TranslateLessonCardTypeService(
            lessonProgressRepository = lessonProgressRepository,
            dictionaryRepository = dictionaryRepository,
            cardHistoryRepository = cardHistoryRepository
        )

        lessonCardService = LessonCardService(
            lessonRepository = lessonRepository,
            lessonCardTypeRepository = lessonCardTypeRepository,
            dictionaryCollectionRepository = dictionaryCollectionRepository,
            dictionaryRepository = dictionaryRepository,
            cardHistoryRepository = cardHistoryRepository,
            lessonProgressRepository = lessonProgressRepository,
            typeServices = mapOf(CardTypeEnum.TRANSLATE to translateService)
        )
    }

    @Test
    fun createProgress_generatesProgressOnlyForNewBaseWords() = runBlocking {
        lessonProgressDao.storage.add(
            LessonProgress(
                id = 42,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.TRANSLATE,
                wordId = 2,
                nextReviewDate = 1_000L,
                intervalMinutes = 15,
                ef = 2.6,
                status = StatusEnum.IN_PROGRESS,
                info = "existing"
            )
        )

        lessonCardService.createProgress(LESSON_ID)

        val progress = lessonProgressDao.getByLessonCardType(LESSON_ID, CardTypeEnum.TRANSLATE)
        val wordIds = progress.mapNotNull { it.wordId }

        assertEquals("Should keep existing entries and add only new base words", 2, wordIds.size)
        assertEquals(1, wordIds.count { it == 1 })
        assertEquals("Existing entry must not be duplicated", 1, wordIds.count { it == 2 })
        val newEntry = progress.first { it.wordId == 1 }
        assertEquals(StatusEnum.NEW, newEntry.status)
    }

    @Test
    fun getCards_returnsTranslateDtos() = runBlocking {
        lessonProgressDao.storage.add(
            LessonProgress(
                id = 1,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.TRANSLATE,
                wordId = 1,
                nextReviewDate = 123L,
                intervalMinutes = 10,
                ef = 2.5,
                status = StatusEnum.NEW,
                info = "info1"
            )
        )
        lessonProgressDao.storage.add(
            LessonProgress(
                id = 2,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.TRANSLATE,
                wordId = 2,
                nextReviewDate = 456L,
                intervalMinutes = 20,
                ef = 2.7,
                status = StatusEnum.IN_PROGRESS,
                info = "info2"
            )
        )

        val cards = lessonCardService.getCards(LESSON_ID)

        assertEquals(2, cards.size)
        val translateCards = cards.filterIsInstance<TranslateLessonCardDto>()
        assertEquals(2, translateCards.size)
        assertEquals(setOf(1, 2), translateCards.mapNotNull { it.wordId }.toSet())
        val words = translateCards.map { it.word }.toSet()
        assertEquals(setOf("base-one", "base-two"), words)
    }

    @Test
    fun getCards_filtersOutNotReadyCards() = runBlocking {
        val now = System.currentTimeMillis()
        lessonProgressDao.storage.add(
            LessonProgress(
                id = 101,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.TRANSLATE,
                wordId = 1,
                nextReviewDate = now - 1_000L,
                intervalMinutes = 10,
                ef = 2.5,
                status = StatusEnum.IN_PROGRESS,
                info = null
            )
        )
        lessonProgressDao.storage.add(
            LessonProgress(
                id = 102,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.TRANSLATE,
                wordId = 2,
                nextReviewDate = now + TimeUnit.DAYS.toMillis(1),
                intervalMinutes = 20,
                ef = 2.5,
                status = StatusEnum.IN_PROGRESS,
                info = null
            )
        )

        val cards = lessonCardService.getCards(LESSON_ID)

        assertEquals(1, cards.size)
        val translateCard = cards.first() as TranslateLessonCardDto
        assertEquals(101, translateCard.progressId)
    }

    @Test
    fun getCards_conditionOnEnablesTypeWhenThresholdReached() = runBlocking {
        val originalTypes = lessonCardTypeDao.cardTypes[COLLECTION_ID]?.map { it.copy() } ?: emptyList()
        try {
            lessonCardTypeDao.cardTypes[COLLECTION_ID] = mutableListOf(
                LessonCardType(collectionId = COLLECTION_ID, cardType = CardTypeEnum.TRANSLATE),
                LessonCardType(
                    collectionId = COLLECTION_ID,
                    cardType = CardTypeEnum.REVERSE_TRANSLATE,
                    conditionOnCardType = CardTypeEnum.TRANSLATE,
                    conditionOnValue = 2
                )
            )

            val reverseService = FakeLessonCardTypeService(
                listOf(DummyLessonCardDto("reverse", CardTypeEnum.REVERSE_TRANSLATE))
            )
            val conditionalService = LessonCardService(
                lessonRepository = lessonRepository,
                lessonCardTypeRepository = lessonCardTypeRepository,
                dictionaryCollectionRepository = dictionaryCollectionRepository,
                dictionaryRepository = dictionaryRepository,
                cardHistoryRepository = cardHistoryRepository,
                lessonProgressRepository = lessonProgressRepository,
                typeServices = mapOf(
                    CardTypeEnum.TRANSLATE to translateService,
                    CardTypeEnum.REVERSE_TRANSLATE to reverseService
                )
            )

            lessonProgressDao.storage.add(
                LessonProgress(
                    id = 201,
                    lessonId = LESSON_ID,
                    cardType = CardTypeEnum.TRANSLATE,
                    wordId = 1,
                    nextReviewDate = System.currentTimeMillis() - 500L,
                    intervalMinutes = 10,
                    ef = 2.5,
                    status = StatusEnum.NEW,
                    info = null
                )
            )

            val withoutHistory = conditionalService.getCards(LESSON_ID)
            assertTrue(withoutHistory.none { it is DummyLessonCardDto })

            cardHistoryDao.storage.addAll(
                listOf(
                    CardHistory(
                        lessonId = LESSON_ID,
                        cardType = CardTypeEnum.TRANSLATE,
                        wordId = 1,
                        quality = 1,
                        date = 10L
                    ),
                    CardHistory(
                        lessonId = LESSON_ID,
                        cardType = CardTypeEnum.TRANSLATE,
                        wordId = 2,
                        quality = 3,
                        date = 20L
                    )
                )
            )

            val withHistory = conditionalService.getCards(LESSON_ID)
            assertTrue(withHistory.any { it is DummyLessonCardDto })
        } finally {
            lessonCardTypeDao.cardTypes[COLLECTION_ID] = originalTypes.toMutableList()
            cardHistoryDao.storage.clear()
        }
    }

    @Test
    fun getCards_conditionOffDisablesTypeWhenThresholdReached() = runBlocking {
        val originalTypes = lessonCardTypeDao.cardTypes[COLLECTION_ID]?.map { it.copy() } ?: emptyList()
        try {
            lessonCardTypeDao.cardTypes[COLLECTION_ID] = mutableListOf(
                LessonCardType(collectionId = COLLECTION_ID, cardType = CardTypeEnum.TRANSLATE),
                LessonCardType(
                    collectionId = COLLECTION_ID,
                    cardType = CardTypeEnum.REVERSE_TRANSLATE,
                    conditionOffCardType = CardTypeEnum.TRANSLATE,
                    conditionOffValue = 1
                )
            )

            val reverseService = FakeLessonCardTypeService(
                listOf(DummyLessonCardDto("reverse", CardTypeEnum.REVERSE_TRANSLATE))
            )
            val conditionalService = LessonCardService(
                lessonRepository = lessonRepository,
                lessonCardTypeRepository = lessonCardTypeRepository,
                dictionaryCollectionRepository = dictionaryCollectionRepository,
                dictionaryRepository = dictionaryRepository,
                cardHistoryRepository = cardHistoryRepository,
                lessonProgressRepository = lessonProgressRepository,
                typeServices = mapOf(
                    CardTypeEnum.TRANSLATE to translateService,
                    CardTypeEnum.REVERSE_TRANSLATE to reverseService
                )
            )

            lessonProgressDao.storage.add(
                LessonProgress(
                    id = 301,
                    lessonId = LESSON_ID,
                    cardType = CardTypeEnum.TRANSLATE,
                    wordId = 1,
                    nextReviewDate = System.currentTimeMillis() - 500L,
                    intervalMinutes = 10,
                    ef = 2.5,
                    status = StatusEnum.NEW,
                    info = null
                )
            )

            val initial = conditionalService.getCards(LESSON_ID)
            assertTrue(initial.any { it is DummyLessonCardDto })

            cardHistoryDao.storage.add(
                CardHistory(
                    lessonId = LESSON_ID,
                    cardType = CardTypeEnum.TRANSLATE,
                    wordId = 1,
                    quality = 4,
                    date = 30L
                )
            )

            val afterThreshold = conditionalService.getCards(LESSON_ID)
            assertTrue(afterThreshold.none { it is DummyLessonCardDto })
        } finally {
            lessonCardTypeDao.cardTypes[COLLECTION_ID] = originalTypes.toMutableList()
            cardHistoryDao.storage.clear()
        }
    }

    @Test
    fun getCards_combinesAllRegisteredTypeServices() = runBlocking {
        lessonCardTypeDao.cardTypes[COLLECTION_ID] = mutableListOf(
            LessonCardType(collectionId = COLLECTION_ID, cardType = CardTypeEnum.TRANSLATE),
            LessonCardType(collectionId = COLLECTION_ID, cardType = CardTypeEnum.REVERSE_TRANSLATE)
        )

        val reverseService = FakeLessonCardTypeService(
            listOf(DummyLessonCardDto("reverse", CardTypeEnum.REVERSE_TRANSLATE))
        )
        val aggregatorService = LessonCardService(
            lessonRepository = lessonRepository,
            lessonCardTypeRepository = lessonCardTypeRepository,
            dictionaryCollectionRepository = dictionaryCollectionRepository,
            dictionaryRepository = dictionaryRepository,
            cardHistoryRepository = cardHistoryRepository,
            lessonProgressRepository = lessonProgressRepository,
            typeServices = mapOf(
                CardTypeEnum.TRANSLATE to translateService,
                CardTypeEnum.REVERSE_TRANSLATE to reverseService
            )
        )

        lessonProgressDao.storage.add(
            LessonProgress(
                id = 10,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.TRANSLATE,
                wordId = 1,
                nextReviewDate = 100L,
                intervalMinutes = 0,
                ef = 2.5,
                status = StatusEnum.NEW,
                info = null
            )
        )
        lessonProgressDao.storage.add(
            LessonProgress(
                id = 11,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.REVERSE_TRANSLATE,
                wordId = 2,
                nextReviewDate = 200L,
                intervalMinutes = 0,
                ef = 2.4,
                status = StatusEnum.NEW,
                info = null
            )
        )

        val cards = aggregatorService.getCards(LESSON_ID)

        assertEquals(2, cards.size)
        assertTrue(cards.any { it is TranslateLessonCardDto })
        assertTrue(cards.any { it is DummyLessonCardDto })
    }

    @Test
    fun answerResult_delegatesToRegisteredTypeService() = runBlocking {
        val progress = LessonProgress(
            id = 501,
            lessonId = LESSON_ID,
            cardType = CardTypeEnum.TRANSLATE,
            wordId = 1,
            nextReviewDate = 0L,
            intervalMinutes = 1440,
            ef = 2.5,
            status = StatusEnum.IN_PROGRESS,
            info = "original"
        )
        lessonProgressDao.storage.add(progress)

        val expectedResultCard = DummyLessonCardDto("updated", CardTypeEnum.TRANSLATE)
        val recordingService = RecordingLessonCardTypeService(
            onAnswerResult = { _, _, _, _ -> expectedResultCard }
        )
        val delegatingService = LessonCardService(
            lessonRepository = lessonRepository,
            lessonCardTypeRepository = lessonCardTypeRepository,
            dictionaryCollectionRepository = dictionaryCollectionRepository,
            dictionaryRepository = dictionaryRepository,
            cardHistoryRepository = cardHistoryRepository,
            lessonProgressRepository = lessonProgressRepository,
            typeServices = mapOf(CardTypeEnum.TRANSLATE to recordingService),
            timeProvider = { 1234L }
        )

        val card = DummyLessonCardDto("original", CardTypeEnum.TRANSLATE)
        val result = delegatingService.answerResult(card, null, 4)

        assertEquals(expectedResultCard, result)
        assertEquals(card, recordingService.lastCard)
        assertEquals(null, recordingService.lastAnswer)
        assertEquals(4, recordingService.lastQuality)
        assertEquals(1234L, recordingService.lastTimestamp)
    }

    @Test
    fun answerResult_logsHistoryForTranslateCard() = runBlocking {
        val progressId = 600
        lessonProgressDao.storage.add(
            LessonProgress(
                id = progressId,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.TRANSLATE,
                wordId = 1,
                nextReviewDate = 0L,
                intervalMinutes = 1440,
                ef = 2.5,
                status = StatusEnum.NEW,
                info = null
            )
        )

        val card = TranslateLessonCardDto(
            progressId = progressId,
            lessonId = LESSON_ID,
            wordId = 1,
            word = "word",
            translation = "перевод",
            hint = null,
            imagePath = null,
            status = StatusEnum.NEW,
            intervalMinutes = 1440,
            ef = 2.5,
            nextReviewDate = null,
            info = null
        )

        val result = lessonCardService.answerResult(card, null, 5)

        assertEquals(null, result)
        val history = cardHistoryDao.storage
        assertEquals(1, history.size)
        val entry = history.first()
        assertEquals(LESSON_ID, entry.lessonId)
        assertEquals(CardTypeEnum.TRANSLATE, entry.cardType)
        assertEquals(1, entry.wordId)
        assertEquals(5, entry.quality)
        assertTrue(entry.date > 0)
    }

    @Test
    fun answerResult_logsHistoryForTranslationComparisonCard() = runBlocking {
        val comparisonService = RecordingLessonCardTypeService(
            cardsToReturn = emptyList(),
            onAnswerResult = { _, _, _, _ -> null }
        )
        val comparisonLessonCardService = LessonCardService(
            lessonRepository = lessonRepository,
            lessonCardTypeRepository = lessonCardTypeRepository,
            dictionaryCollectionRepository = dictionaryCollectionRepository,
            dictionaryRepository = dictionaryRepository,
            cardHistoryRepository = cardHistoryRepository,
            lessonProgressRepository = lessonProgressRepository,
            typeServices = mapOf(CardTypeEnum.TRANSLATION_COMPARISON to comparisonService),
            timeProvider = { 9999L }
        )

        val card = TranslationComparisonLessonCardDto(
            lessonId = LESSON_ID,
            items = listOf(
                TranslationComparisonLessonCardDto.Item(
                    progressId = 1,
                    wordId = 1,
                    word = "one",
                    translation = "раз",
                    hint = null,
                    imagePath = null,
                    status = StatusEnum.NEW,
                    intervalMinutes = 10,
                    ef = 2.5,
                    nextReviewDate = null,
                    info = null
                ),
                TranslationComparisonLessonCardDto.Item(
                    progressId = 2,
                    wordId = 2,
                    word = "two",
                    translation = "два",
                    hint = null,
                    imagePath = null,
                    status = StatusEnum.NEW,
                    intervalMinutes = 10,
                    ef = 2.5,
                    nextReviewDate = null,
                    info = null
                )
            )
        )
        val answer = CardAnswer.Comparison(
            matches = listOf(
                CardAnswer.Comparison.Match(progressId = 1, selectedWordId = 1),
                CardAnswer.Comparison.Match(progressId = 2, selectedWordId = 42)
            )
        )

        val result = comparisonLessonCardService.answerResult(card, answer, 5)

        assertEquals(null, result)
        val historyByWord = cardHistoryDao.storage.associateBy { it.wordId }
        assertEquals(2, historyByWord.size)
        assertEquals(5, historyByWord[1]?.quality)
        assertEquals(0, historyByWord[2]?.quality)
        assertEquals(9999L, historyByWord.values.firstOrNull()?.date)
    }

    private class FakeLessonDao : LessonDao {
        val lessons = mutableMapOf<Int, Lesson>()
        private var nextId = 1000

        override suspend fun insert(lesson: Lesson): Long {
            val id = if (lesson.id == 0) nextId++ else lesson.id
            lessons[id] = lesson.copy(id = id)
            return id.toLong()
        }

        override suspend fun update(lesson: Lesson) {
            lessons[lesson.id] = lesson
        }

        override suspend fun delete(lesson: Lesson) {
            lessons.remove(lesson.id)
        }

        override suspend fun getAll(): List<Lesson> = lessons.values.toList()

        override suspend fun getById(id: Int): Lesson? = lessons[id]

        override suspend fun getByCollectionId(collectionId: Int): List<Lesson> =
            lessons.values.filter { it.collectionId == collectionId }
    }

    private class FakeLessonCardTypeDao : LessonCardTypeDao {
        val cardTypes = mutableMapOf<Int, MutableList<LessonCardType>>()

        override suspend fun insert(entity: LessonCardType) {
            val list = cardTypes.getOrPut(entity.collectionId) { mutableListOf() }
            list.removeIf { it.cardType == entity.cardType }
            list.add(entity)
        }

        override suspend fun insertAll(entities: List<LessonCardType>) {
            entities.forEach { insert(it) }
        }

        override suspend fun delete(entity: LessonCardType) {
            cardTypes[entity.collectionId]?.removeIf { it.cardType == entity.cardType }
        }

        override suspend fun deleteByCollectionId(collectionId: Int) {
            cardTypes.remove(collectionId)
        }

        override suspend fun getAll(): List<LessonCardType> = cardTypes.values.flatten()

        override suspend fun getByCollectionId(collectionId: Int): List<LessonCardType> =
            cardTypes[collectionId]?.toList() ?: emptyList()

        override suspend fun replaceForCollection(collectionId: Int, entities: List<LessonCardType>) {
            cardTypes[collectionId] = entities.toMutableList()
        }
    }

    private class FakeWordCollectionDao : WordCollectionDao {
        override suspend fun insert(collection: WordCollection): Long = collection.id.toLong()
        override suspend fun delete(collection: WordCollection) = Unit
        override suspend fun getAll(): List<WordCollection> = emptyList()
        override suspend fun getById(id: Int): WordCollection? = null
        override suspend fun getByName(name: String): WordCollection? = null
        override suspend fun getAllWithEntries(): List<WordCollectionWithEntries> = emptyList()
        override suspend fun getByIdWithEntries(id: Int): WordCollectionWithEntries? = null
    }

    private class FakeWordCollectionEntryDao : WordCollectionEntryDao {
        val entries = mutableMapOf<Int, MutableList<WordCollectionEntry>>()

        override suspend fun insert(entry: WordCollectionEntry): Long {
            entries.getOrPut(entry.collectionId) { mutableListOf() }.add(entry)
            return 0L
        }

        override suspend fun insertAll(entries: List<WordCollectionEntry>): List<Long> {
            entries.forEach { insert(it) }
            return emptyList()
        }

        override suspend fun delete(entry: WordCollectionEntry) {
            entries[entry.collectionId]?.removeIf { it.wordId == entry.wordId }
        }

        override suspend fun deleteByCollectionId(collectionId: Int) {
            entries.remove(collectionId)
        }

        override suspend fun deleteByCollectionAndWord(collectionId: Int, wordId: Int) {
            entries[collectionId]?.removeIf { it.wordId == wordId }
        }

        override suspend fun getByCollectionId(collectionId: Int): List<WordCollectionEntry> =
            entries[collectionId]?.toList() ?: emptyList()
    }

    private class FakeWordDao(
        initialData: MutableMap<Int, Word>
    ) : WordDao {

        val dictionaries: MutableMap<Int, Word> = initialData
        private var nextId = (initialData.keys.maxOrNull() ?: 0) + 1

        override suspend fun insert(entry: Word): Long {
            val id = if (entry.id == 0) nextId++ else entry.id
            dictionaries[id] = entry.copy(id = id)
            return id.toLong()
        }

        override suspend fun delete(entry: Word) {
            dictionaries.remove(entry.id)
        }

        override suspend fun getAll(): List<Word> = dictionaries.values.toList()

        override suspend fun getBaseEntries(): List<Word> =
            dictionaries.values.filter { it.baseWordId == null || it.baseWordId == it.id }

        override suspend fun getById(id: Int): Word? = dictionaries[id]

        override suspend fun getByWord(word: String): List<Word> =
            dictionaries.values.filter { it.word.equals(word, ignoreCase = true) }

        override suspend fun getByBaseWordId(baseWordId: Int): List<Word> =
            dictionaries.values.filter { it.baseWordId == baseWordId }

        override suspend fun getGroupByBaseId(baseWordId: Int): List<Word> =
            dictionaries.values.filter { it.baseWordId == baseWordId || it.id == baseWordId }

        override suspend fun getByIds(ids: List<Int>): List<Word> =
            ids.mapNotNull { dictionaries[it] }

        override suspend fun getBaseEntriesWithDependentCount(): List<WordWithDependentCount> {
            val baseEntries = getBaseEntries()
            return baseEntries.map { base ->
                val dependentCount = dictionaries.values.count { entry ->
                    entry.baseWordId == base.id && entry.id != base.id
                }
                WordWithDependentCount(base, dependentCount)
            }
        }

        override suspend fun getGroupByEntryId(entryId: Int): List<Word> {
            val baseId = dictionaries[entryId]?.baseWordId ?: entryId
            val resolvedBaseId = if (baseId == 0) entryId else baseId
            return dictionaries.values.filter { entry ->
                entry.id == resolvedBaseId || entry.baseWordId == resolvedBaseId
            }.sortedBy { it.id }
        }
    }

    private class FakeLessonProgressDao : LessonProgressDao {
        private var nextId = 1
        val storage = mutableListOf<LessonProgress>()

        override suspend fun insert(progress: LessonProgress): Long {
            val assigned = ensureId(progress)
            storage.add(assigned)
            return assigned.id.toLong()
        }

        override suspend fun insertAll(progressList: List<LessonProgress>): List<Long> =
            progressList.map { insert(it) }

        override suspend fun update(progress: LessonProgress) {
            val idx = storage.indexOfFirst { it.id == progress.id }
            if (idx >= 0) storage[idx] = progress
        }

        override suspend fun delete(progress: LessonProgress) {
            storage.removeIf { it.id == progress.id }
        }

        override suspend fun deleteByLessonCardType(lessonId: Int, cardType: CardTypeEnum) {
            storage.removeIf { it.lessonId == lessonId && it.cardType == cardType }
        }

        override suspend fun getAll(): List<LessonProgress> = storage.toList()

        override suspend fun getByLessonCardType(lessonId: Int, cardType: CardTypeEnum): List<LessonProgress> =
            storage.filter { it.lessonId == lessonId && it.cardType == cardType }

        override suspend fun getByWordId(wordId: Int): List<LessonProgress> =
            storage.filter { it.wordId == wordId }

        override suspend fun getById(id: Int): LessonProgress? =
            storage.firstOrNull { it.id == id }

        private fun ensureId(progress: LessonProgress): LessonProgress {
            if (progress.id != 0) return progress
            val id = nextId++
            return progress.copy(id = id)
        }
    }

    private class FakeCardHistoryDao : CardHistoryDao {
        private var nextId = 1
        val storage = mutableListOf<CardHistory>()

        override suspend fun insert(entry: CardHistory) {
            storage.add(assignId(entry))
        }

        override suspend fun insertAll(entries: List<CardHistory>) {
            entries.forEach { insert(it) }
        }

        override suspend fun getByLesson(lessonId: Int): List<CardHistory> =
            storage.filter { it.lessonId == lessonId }.sortedByDescending { it.date }

        override suspend fun getByLessonAndType(lessonId: Int, cardType: CardTypeEnum): List<CardHistory> =
            storage.filter { it.lessonId == lessonId && it.cardType == cardType }
                .sortedByDescending { it.date }

        override suspend fun getByWord(wordId: Int): List<CardHistory> =
            storage.filter { it.wordId == wordId }.sortedByDescending { it.date }

        override suspend fun clearAll() {
            storage.clear()
        }

        override suspend fun clearByLesson(lessonId: Int) {
            storage.removeIf { it.lessonId == lessonId }
        }

        private fun assignId(entry: CardHistory): CardHistory {
            val id = if (entry.id == 0) nextId++ else entry.id
            return entry.copy(id = id)
        }
    }

    private class FakeLessonCardTypeService(
        private val cardsToReturn: List<LessonCardDto>
    ) : LessonCardTypeService {
        override suspend fun createProgress(
            lessonId: Int,
            words: List<Word>
        ) {
            // no-op for tests
        }

        override suspend fun getCards(cardProgress: List<LessonProgress>): List<LessonCardDto> =
            cardsToReturn

        override suspend fun answerResult(
            card: LessonCardDto,
            answer: CardAnswer?,
            quality: Int,
            currentTimeMillis: Long
        ): LessonCardDto? = null
    }

    private class RecordingLessonCardTypeService(
        private val cardsToReturn: List<LessonCardDto> = emptyList(),
        private val onAnswerResult: (LessonCardDto, CardAnswer?, Int, Long) -> LessonCardDto?
    ) : LessonCardTypeService {

        var lastCard: LessonCardDto? = null
        var lastAnswer: CardAnswer? = null
        var lastQuality: Int? = null
        var lastTimestamp: Long? = null

        override suspend fun createProgress(lessonId: Int, words: List<Word>) {
            // no-op
        }

        override suspend fun getCards(cardProgress: List<LessonProgress>): List<LessonCardDto> =
            cardsToReturn

        override suspend fun answerResult(
            card: LessonCardDto,
            answer: CardAnswer?,
            quality: Int,
            currentTimeMillis: Long
        ): LessonCardDto? {
            lastCard = card
            lastAnswer = answer
            lastQuality = quality
            lastTimestamp = currentTimeMillis
            return onAnswerResult(card, answer, quality, currentTimeMillis)
        }
    }

    private data class DummyLessonCardDto(
        val label: String,
        override val type: CardTypeEnum
    ) : LessonCardDto

    private companion object {
        private const val LESSON_ID = 1
        private const val COLLECTION_ID = 100
    }
}

