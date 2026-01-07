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
import com.example.tala.model.dto.lessonCard.TranslationComparisonLessonCardDto
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import com.example.tala.service.lessonCard.model.CardAnswer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class TranslationComparisonLessonCardTypeServiceTest {

    private lateinit var progressDao: RecordingLessonProgressDao
    private lateinit var progressRepository: LessonProgressRepository
    private lateinit var dictionaryRepository: WordRepository
    private lateinit var cardHistoryRepository: CardHistoryRepository
    private lateinit var service: TranslationComparisonLessonCardTypeService

    @Before
    fun setUp() {
        progressDao = RecordingLessonProgressDao()
        progressRepository = LessonProgressRepository(progressDao)
        dictionaryRepository = WordRepository(FakeWordDao())
        cardHistoryRepository = CardHistoryRepository(FakeCardHistoryDao())
        service = TranslationComparisonLessonCardTypeService(
            progressRepository,
            dictionaryRepository,
            cardHistoryRepository
        )
    }

    @Test
    fun getCards_returnsChunkedGroups() = runBlocking {
        val progresses = (1..5).map { idx ->
            createProgress(id = idx, wordId = idx)
        }
        progressDao.storage.addAll(progresses)

        val cards = service.getCards(progresses)

        assertEquals(1, cards.size)
        val card = cards.first() as TranslationComparisonLessonCardDto
        assertEquals(5, card.items.size)
        val idsFromCard = card.items.map { it.progressId }.toSet()
        assertEquals(progresses.map { it.id }.toSet(), idsFromCard)
    }

    @Test
    fun answerResult_allCorrect_returnsNullAndAdvancesProgress() = runBlocking {
        val progresses = (1..3).map { idx ->
            createProgress(id = idx, wordId = idx, status = StatusEnum.NEW)
        }
        progressDao.storage.addAll(progresses)

        val card = TranslationComparisonLessonCardDto.fromProgress(
            lessonId = 1,
            progresses = progresses,
            words = emptyMap()
        )
        val matches = card.items.map { CardAnswer.Comparison.Match(it.progressId, it.wordId) }
        val result = service.answerResult(card, CardAnswer.Comparison(matches), 5, NOW)

        assertNull(result)
        progresses.forEach {
            val updated = progressDao.storage.first { stored -> stored.id == it.id }
            assertEquals(StatusEnum.IN_PROGRESS, updated.status)
        }
    }

    @Test
    fun answerResult_withMistake_returnsCardToRepeat() = runBlocking {
        val progresses = (1..3).map { idx ->
            createProgress(id = idx, wordId = idx, status = StatusEnum.IN_PROGRESS)
        }
        progressDao.storage.addAll(progresses)

        val card = TranslationComparisonLessonCardDto.fromProgress(
            lessonId = 1,
            progresses = progresses,
            words = emptyMap()
        )
        val matches = card.items.mapIndexed { index, item ->
            val selected = if (index == 0) item.wordId?.plus(100) else item.wordId
            CardAnswer.Comparison.Match(item.progressId, selected)
        }
        val result = service.answerResult(card, CardAnswer.Comparison(matches), 5, NOW)

        assertNotNull(result)
        val returnedCard = result as TranslationComparisonLessonCardDto
        assertEquals(card.items.map { it.progressId }.toSet(), returnedCard.items.map { it.progressId }.toSet())

        val incorrectProgress = progressDao.storage.first { it.id == card.items.first().progressId }
        assertEquals(StatusEnum.PROGRESS_RESET, incorrectProgress.status)
        val correctProgress = progressDao.storage.first { it.id == card.items[1].progressId }
        assertEquals(StatusEnum.IN_PROGRESS, correctProgress.status)
    }

    private fun createProgress(
        id: Int,
        wordId: Int,
        status: StatusEnum = StatusEnum.NEW,
        interval: Long = TimeUnit.DAYS.toMinutes(1)
    ): LessonProgress = LessonProgress(
        id = id,
        lessonId = 1,
        cardType = CardTypeEnum.TRANSLATION_COMPARISON,
        wordId = wordId,
        nextReviewDate = NOW,
        intervalMinutes = interval,
        ef = 2.5,
        status = status,
        info = null
    )

    private class RecordingLessonProgressDao : LessonProgressDao {
        val storage = mutableListOf<LessonProgress>()
        private var nextId = 100

        override suspend fun insert(progress: LessonProgress): Long {
            val saved = ensureId(progress)
            storage.add(saved)
            return saved.id.toLong()
        }

        override suspend fun insertAll(progressList: List<LessonProgress>): List<Long> {
            progressList.forEach { insert(it) }
            return progressList.map { it.id.toLong() }
        }

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
            val assigned = progress.copy(id = nextId++)
            return assigned
        }
    }

    private class FakeWordDao : WordDao {
        override suspend fun insert(entry: Word): Long = entry.id.toLong()
        override suspend fun delete(entry: Word) = Unit
        override suspend fun getAll(): List<Word> = emptyList()
        override suspend fun getBaseEntries(): List<Word> = emptyList()
        override suspend fun getById(id: Int): Word? = Word(
            id = id,
            word = "word$id",
            translation = "translation$id",
            partOfSpeech = com.example.tala.entity.word.PartOfSpeech.NOUN
        )

        override suspend fun getByWord(word: String): List<Word> = emptyList()
        override suspend fun getByBaseWordId(baseWordId: Int): List<Word> = emptyList()
        override suspend fun getGroupByBaseId(baseWordId: Int): List<Word> = emptyList()
        override suspend fun getByIds(ids: List<Int>): List<Word> =
            ids.mapNotNull { getById(it) }

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

