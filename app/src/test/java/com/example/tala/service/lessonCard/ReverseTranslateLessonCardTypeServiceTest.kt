package com.example.tala.service.lessonCard

import com.example.tala.entity.cardhistory.CardHistory
import com.example.tala.entity.cardhistory.CardHistoryDao
import com.example.tala.entity.cardhistory.CardHistoryRepository
import com.example.tala.entity.word.Word
import com.example.tala.entity.word.WordDao
import com.example.tala.entity.word.WordRepository
import com.example.tala.entity.word.WordWithDependentCount
import com.example.tala.entity.lessonprogress.LessonProgress
import com.example.tala.entity.lessonprogress.LessonProgressDao
import com.example.tala.entity.lessonprogress.LessonProgressRepository
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import com.example.tala.model.dto.lessonCard.ReverseTranslateLessonCardDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

class ReverseTranslateLessonCardTypeServiceTest {

    private lateinit var progressDao: RecordingLessonProgressDao
    private lateinit var progressRepository: LessonProgressRepository
    private lateinit var cardHistoryRepository: CardHistoryRepository
    private lateinit var service: ReverseTranslateLessonCardTypeService

    @Before
    fun setUp() {
        progressDao = RecordingLessonProgressDao()
        progressRepository = LessonProgressRepository(progressDao)
        cardHistoryRepository = CardHistoryRepository(FakeCardHistoryDao())
        service = ReverseTranslateLessonCardTypeService(
            progressRepository,
            WordRepository(FakeWordDao()),
            cardHistoryRepository
        )
    }

    @Test
    fun answerResult_qualityZero_movesToResetAndSchedulesSoon() = runBlocking {
        val progress = createProgress(status = StatusEnum.IN_PROGRESS, ef = 2.5, interval = 2880L)
        progressDao.storage.add(progress)
        val card = ReverseTranslateLessonCardDto.fromProgress(progress, null)

        val repeatCard = service.answerResult(card, null, 0, NOW)

        val updated = progressDao.lastUpdated!!
        val expectedEf = maxOf(1.3, 2.5 - 0.8)
        assertEquals(StatusEnum.PROGRESS_RESET, updated.status)
        assertEquals(1440L, updated.intervalMinutes)
        assertEquals(expectedEf, updated.ef, 0.0001)
        assertEquals(NOW + TimeUnit.MINUTES.toMillis(10), updated.nextReviewDate)
        assertTrue(repeatCard is ReverseTranslateLessonCardDto)
    }

    @Test
    fun answerResult_qualityZeroRepeated_errorDoesNotChangeEf() = runBlocking {
        val progress = createProgress(status = StatusEnum.PROGRESS_RESET, ef = 1.6, interval = 1440L)
        progressDao.storage.add(progress)
        val card = ReverseTranslateLessonCardDto.fromProgress(progress, null)

        val repeatCard = service.answerResult(card, null, 0, NOW)

        val updated = progressDao.lastUpdated!!
        assertEquals(StatusEnum.PROGRESS_RESET, updated.status)
        assertEquals(1440L, updated.intervalMinutes)
        assertEquals(1.6, updated.ef, 0.0001)
        assertEquals(NOW + TimeUnit.MINUTES.toMillis(10), updated.nextReviewDate)
        assertTrue(repeatCard is ReverseTranslateLessonCardDto)
    }

    @Test
    fun answerResult_firstSuccess_movesToInProgressWithoutChangingEf() = runBlocking {
        val progress = createProgress(status = StatusEnum.NEW, ef = 2.3, interval = 1440L)
        progressDao.storage.add(progress)
        val card = ReverseTranslateLessonCardDto.fromProgress(progress, null)

        val repeatCard = service.answerResult(card, null, 5, NOW)

        val updated = progressDao.lastUpdated!!
        assertEquals(StatusEnum.IN_PROGRESS, updated.status)
        assertEquals(1440L, updated.intervalMinutes)
        assertEquals(2.3, updated.ef, 0.0001)
        assertEquals(NOW + TimeUnit.DAYS.toMillis(1), updated.nextReviewDate)
        assertEquals(null, repeatCard)
    }

    @Test
    fun answerResult_subsequentSuccess_appliesSm2Formula() = runBlocking {
        val progress = createProgress(
            status = StatusEnum.IN_PROGRESS,
            ef = 2.5,
            interval = 1440L,
            nextReview = NOW - TimeUnit.DAYS.toMillis(1)
        )
        progressDao.storage.add(progress)
        val card = ReverseTranslateLessonCardDto.fromProgress(progress, null)

        val repeatCard = service.answerResult(card, null, 5, NOW)

        val updated = progressDao.lastUpdated!!
        val expectedEf = 2.5 + 0.1 // q = 5
        val expectedInterval = (1440L * expectedEf).roundToLong()
        assertEquals(StatusEnum.IN_PROGRESS, updated.status)
        assertEquals(expectedEf, updated.ef, 0.0001)
        assertEquals(expectedInterval, updated.intervalMinutes)
        assertEquals(NOW + TimeUnit.MINUTES.toMillis(expectedInterval), updated.nextReviewDate)
        assertTrue("Interval should grow", updated.intervalMinutes > progress.intervalMinutes)
        assertEquals(null, repeatCard)
    }

    private fun createProgress(
        status: StatusEnum,
        ef: Double,
        interval: Long,
        nextReview: Long = NOW
    ): LessonProgress = LessonProgress(
        id = progressDao.nextId(),
        lessonId = 1,
        cardType = CardTypeEnum.REVERSE_TRANSLATE,
        wordId = 1,
        nextReviewDate = nextReview,
        intervalMinutes = interval,
        ef = ef,
        status = status,
        info = null
    )

    private class RecordingLessonProgressDao : LessonProgressDao {
        private var idCounter = 1
        val storage = mutableListOf<LessonProgress>()
        var lastUpdated: LessonProgress? = null

        fun nextId(): Int = idCounter++

        override suspend fun insert(progress: LessonProgress): Long {
            val entity = if (progress.id == 0) progress.copy(id = nextId()) else progress
            storage.add(entity)
            return entity.id.toLong()
        }

        override suspend fun insertAll(progressList: List<LessonProgress>): List<Long> {
            return progressList.map { insert(it) }
        }

        override suspend fun update(progress: LessonProgress) {
            lastUpdated = progress
            val index = storage.indexOfFirst { it.id == progress.id }
            if (index >= 0) {
                storage[index] = progress
            } else {
                storage.add(progress)
            }
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
    }

    private class FakeWordDao : WordDao {
        override suspend fun insert(entry: Word): Long = error("Not used")
        override suspend fun delete(entry: Word) = error("Not used")
        override suspend fun getAll(): List<Word> = emptyList()
        override suspend fun getBaseEntries(): List<Word> = emptyList()
        override suspend fun getById(id: Int): Word? = null
        override suspend fun getByWord(word: String): List<Word> = emptyList()
        override suspend fun getByBaseWordId(baseWordId: Int): List<Word> = emptyList()
        override suspend fun getGroupByBaseId(baseWordId: Int): List<Word> = emptyList()
        override suspend fun getByIds(ids: List<Int>): List<Word> = emptyList()
        override suspend fun getBaseEntriesWithDependentCount(): List<WordWithDependentCount> = emptyList()
        override suspend fun getGroupByEntryId(entryId: Int): List<Word> = emptyList()
    }

    private class FakeCardHistoryDao : CardHistoryDao {
        val storage = mutableListOf<CardHistory>()

        override suspend fun insert(entry: CardHistory) {
            storage.add(entry)
        }

        override suspend fun insertAll(entries: List<CardHistory>) {
            storage.addAll(entries)
        }

        override suspend fun getByLesson(lessonId: Int): List<CardHistory> =
            storage.filter { it.lessonId == lessonId }

        override suspend fun getByLessonAndType(lessonId: Int, cardType: CardTypeEnum): List<CardHistory> =
            storage.filter { it.lessonId == lessonId && it.cardType == cardType }

        override suspend fun getByWord(wordId: Int): List<CardHistory> =
            storage.filter { it.wordId == wordId }

        override suspend fun clearAll() {
            storage.clear()
        }

        override suspend fun clearByLesson(lessonId: Int) {
            storage.removeIf { it.lessonId == lessonId }
        }
    }

    companion object {
        private const val NOW = 1_000_000L
    }
}

