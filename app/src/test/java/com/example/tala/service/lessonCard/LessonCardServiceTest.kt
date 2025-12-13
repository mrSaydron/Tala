package com.example.tala.service.lessonCard

import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.DictionaryDao
import com.example.tala.entity.dictionary.DictionaryRepository
import com.example.tala.entity.dictionary.DictionaryWithDependentCount
import com.example.tala.entity.dictionary.PartOfSpeech
import com.example.tala.entity.dictionaryCollection.DictionaryCollection
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionDao
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionEntry
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionEntryDao
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionRepository
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionWithEntries
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
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LessonCardServiceTest {

    private lateinit var lessonDao: FakeLessonDao
    private lateinit var lessonCardTypeDao: FakeLessonCardTypeDao
    private lateinit var dictionaryCollectionDao: FakeDictionaryCollectionDao
    private lateinit var dictionaryCollectionEntryDao: FakeDictionaryCollectionEntryDao
    private lateinit var dictionaryDao: FakeDictionaryDao
    private lateinit var lessonProgressDao: FakeLessonProgressDao

    private lateinit var lessonRepository: LessonRepository
    private lateinit var lessonCardTypeRepository: LessonCardTypeRepository
    private lateinit var dictionaryCollectionRepository: DictionaryCollectionRepository
    private lateinit var dictionaryRepository: DictionaryRepository
    private lateinit var lessonProgressRepository: LessonProgressRepository

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

        dictionaryCollectionDao = FakeDictionaryCollectionDao()

        dictionaryCollectionEntryDao = FakeDictionaryCollectionEntryDao().apply {
            entries[COLLECTION_ID] = mutableListOf(
                DictionaryCollectionEntry(COLLECTION_ID, 1),
                DictionaryCollectionEntry(COLLECTION_ID, 2),
                DictionaryCollectionEntry(COLLECTION_ID, 3)
            )
        }
        dictionaryCollectionRepository = DictionaryCollectionRepository(
            dictionaryCollectionDao,
            dictionaryCollectionEntryDao
        )

        dictionaryDao = FakeDictionaryDao(
            mutableMapOf(
                1 to Dictionary(
                    id = 1,
                    word = "base-one",
                    translation = "перевод один",
                    partOfSpeech = PartOfSpeech.NOUN,
                    baseWordId = null
                ),
                2 to Dictionary(
                    id = 2,
                    word = "base-two",
                    translation = "перевод два",
                    partOfSpeech = PartOfSpeech.NOUN,
                    baseWordId = 2
                ),
                3 to Dictionary(
                    id = 3,
                    word = "derived",
                    translation = "перевод производное",
                    partOfSpeech = PartOfSpeech.NOUN,
                    baseWordId = 1
                )
            )
        )
        dictionaryRepository = DictionaryRepository(dictionaryDao)

        lessonProgressDao = FakeLessonProgressDao()
        lessonProgressRepository = LessonProgressRepository(lessonProgressDao)

        translateService = TranslateLessonCardTypeService(
            lessonProgressRepository,
            dictionaryRepository
        )

        lessonCardService = LessonCardService(
            lessonRepository,
            lessonCardTypeRepository,
            dictionaryCollectionRepository,
            dictionaryRepository,
            lessonProgressRepository,
            mapOf(CardTypeEnum.TRANSLATE to translateService)
        )
    }

    @Test
    fun createProgress_generatesProgressOnlyForNewBaseWords() = runBlocking {
        lessonProgressDao.storage.add(
            LessonProgress(
                id = 42,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.TRANSLATE,
                dictionaryId = 2,
                nextReviewDate = 1_000L,
                intervalMinutes = 15,
                ef = 2.6,
                status = StatusEnum.IN_PROGRESS,
                info = "existing"
            )
        )

        lessonCardService.createProgress(LESSON_ID)

        val progress = lessonProgressDao.getByLessonCardType(LESSON_ID, CardTypeEnum.TRANSLATE)
        val dictionaryIds = progress.mapNotNull { it.dictionaryId }

        assertEquals("Should keep existing entries and add only new base words", 2, dictionaryIds.size)
        assertEquals(1, dictionaryIds.count { it == 1 })
        assertEquals("Existing entry must not be duplicated", 1, dictionaryIds.count { it == 2 })
        val newEntry = progress.first { it.dictionaryId == 1 }
        assertEquals(StatusEnum.NEW, newEntry.status)
    }

    @Test
    fun getCards_returnsTranslateDtos() = runBlocking {
        lessonProgressDao.storage.add(
            LessonProgress(
                id = 1,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.TRANSLATE,
                dictionaryId = 1,
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
                dictionaryId = 2,
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
        assertEquals(setOf(1, 2), translateCards.mapNotNull { it.dictionaryId }.toSet())
        val words = translateCards.map { it.word }.toSet()
        assertEquals(setOf("base-one", "base-two"), words)
    }

    @Test
    fun getCards_combinesAllRegisteredTypeServices() = runBlocking {
        lessonCardTypeDao.cardTypes[COLLECTION_ID] = mutableListOf(
            LessonCardType(collectionId = COLLECTION_ID, cardType = CardTypeEnum.TRANSLATE),
            LessonCardType(collectionId = COLLECTION_ID, cardType = CardTypeEnum.REVERSE_TRANSLATE)
        )

        val reverseService = FakeLessonCardTypeService(listOf(DummyLessonCardDto("reverse")))
        val aggregatorService = LessonCardService(
            lessonRepository,
            lessonCardTypeRepository,
            dictionaryCollectionRepository,
            dictionaryRepository,
            lessonProgressRepository,
            mapOf(
                CardTypeEnum.TRANSLATE to translateService,
                CardTypeEnum.REVERSE_TRANSLATE to reverseService
            )
        )

        lessonProgressDao.storage.add(
            LessonProgress(
                id = 10,
                lessonId = LESSON_ID,
                cardType = CardTypeEnum.TRANSLATE,
                dictionaryId = 1,
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
                dictionaryId = 2,
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

    private class FakeDictionaryCollectionDao : DictionaryCollectionDao {
        override suspend fun insert(collection: DictionaryCollection): Long = collection.id.toLong()
        override suspend fun delete(collection: DictionaryCollection) = Unit
        override suspend fun getAll(): List<DictionaryCollection> = emptyList()
        override suspend fun getById(id: Int): DictionaryCollection? = null
        override suspend fun getByName(name: String): DictionaryCollection? = null
        override suspend fun getAllWithEntries(): List<DictionaryCollectionWithEntries> = emptyList()
        override suspend fun getByIdWithEntries(id: Int): DictionaryCollectionWithEntries? = null
    }

    private class FakeDictionaryCollectionEntryDao : DictionaryCollectionEntryDao {
        val entries = mutableMapOf<Int, MutableList<DictionaryCollectionEntry>>()

        override suspend fun insert(entry: DictionaryCollectionEntry): Long {
            entries.getOrPut(entry.collectionId) { mutableListOf() }.add(entry)
            return 0L
        }

        override suspend fun insertAll(entries: List<DictionaryCollectionEntry>): List<Long> {
            entries.forEach { insert(it) }
            return emptyList()
        }

        override suspend fun delete(entry: DictionaryCollectionEntry) {
            entries[entry.collectionId]?.removeIf { it.dictionaryId == entry.dictionaryId }
        }

        override suspend fun deleteByCollectionId(collectionId: Int) {
            entries.remove(collectionId)
        }

        override suspend fun deleteByCollectionAndDictionary(collectionId: Int, dictionaryId: Int) {
            entries[collectionId]?.removeIf { it.dictionaryId == dictionaryId }
        }

        override suspend fun getByCollectionId(collectionId: Int): List<DictionaryCollectionEntry> =
            entries[collectionId]?.toList() ?: emptyList()
    }

    private class FakeDictionaryDao(
        initialData: MutableMap<Int, Dictionary>
    ) : DictionaryDao {

        val dictionaries: MutableMap<Int, Dictionary> = initialData
        private var nextId = (initialData.keys.maxOrNull() ?: 0) + 1

        override suspend fun insert(entry: Dictionary): Long {
            val id = if (entry.id == 0) nextId++ else entry.id
            dictionaries[id] = entry.copy(id = id)
            return id.toLong()
        }

        override suspend fun delete(entry: Dictionary) {
            dictionaries.remove(entry.id)
        }

        override suspend fun getAll(): List<Dictionary> = dictionaries.values.toList()

        override suspend fun getBaseEntries(): List<Dictionary> =
            dictionaries.values.filter { it.baseWordId == null || it.baseWordId == it.id }

        override suspend fun getById(id: Int): Dictionary? = dictionaries[id]

        override suspend fun getByWord(word: String): List<Dictionary> =
            dictionaries.values.filter { it.word.equals(word, ignoreCase = true) }

        override suspend fun getByBaseWordId(baseWordId: Int): List<Dictionary> =
            dictionaries.values.filter { it.baseWordId == baseWordId }

        override suspend fun getGroupByBaseId(baseWordId: Int): List<Dictionary> =
            dictionaries.values.filter { it.baseWordId == baseWordId || it.id == baseWordId }

        override suspend fun getByIds(ids: List<Int>): List<Dictionary> =
            ids.mapNotNull { dictionaries[it] }

        override suspend fun getBaseEntriesWithDependentCount(): List<DictionaryWithDependentCount> {
            val baseEntries = getBaseEntries()
            return baseEntries.map { base ->
                val dependentCount = dictionaries.values.count { entry ->
                    entry.baseWordId == base.id && entry.id != base.id
                }
                DictionaryWithDependentCount(base, dependentCount)
            }
        }

        override suspend fun getGroupByEntryId(entryId: Int): List<Dictionary> {
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

        override suspend fun getByDictionaryId(dictionaryId: Int): List<LessonProgress> =
            storage.filter { it.dictionaryId == dictionaryId }

        private fun ensureId(progress: LessonProgress): LessonProgress {
            if (progress.id != 0) return progress
            val id = nextId++
            return progress.copy(id = id)
        }
    }

    private class FakeLessonCardTypeService(
        private val cardsToReturn: List<LessonCardDto>
    ) : LessonCardTypeService {
        override suspend fun createProgress(
            lessonId: Int,
            words: List<Dictionary>
        ) {
            // no-op for tests
        }

        override suspend fun getCards(cardProgress: List<LessonProgress>): List<LessonCardDto> =
            cardsToReturn
    }

    private data class DummyLessonCardDto(val label: String) : LessonCardDto

    private companion object {
        private const val LESSON_ID = 1
        private const val COLLECTION_ID = 100
    }
}

