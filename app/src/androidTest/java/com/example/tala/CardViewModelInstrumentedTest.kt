package com.example.tala

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tala.entity.card.Card
import com.example.tala.entity.card.CardDao
import com.example.tala.entity.card.CardViewModel
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class CardViewModelInstrumentedTest {

    private lateinit var app: Application
    private lateinit var viewModel: CardViewModel
    private lateinit var db: TalaDatabase
    private lateinit var cardDao: CardDao

    @Before
    fun setup() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        viewModel = CardViewModel(app)
        db = TalaDatabase.getDatabase(app)
        cardDao = db.cardDao()
        cardDao.deleteAll()
    }

    @After
    fun teardown() {
        // БД одна на приложение, очищаем данные в setup
    }

    private fun endOfTodayEpoch(): Long {
        return LocalDate.now().plusDays(1)
            .atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
    }

    private suspend fun pollUntilTrue(timeoutMs: Long = 2000L, condition: suspend () -> Boolean) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition()) return
            delay(50)
        }
    }

    @Test
    fun resultMedium_updatesAndNotDueSameDay() = runBlocking {
        val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
        val dueCard = Card(
            nextReviewDate = now - 60, // уже просрочена
            categoryId = 1,
            cardType = CardTypeEnum.TRANSLATE,
            intervalMinutes = 1440,
            status = StatusEnum.NEW,
            ef = 2.5
        )
        cardDao.insert(dueCard)

        val endToday = endOfTodayEpoch()
        val fetchedDto = viewModel.getNextCardDtoToReview(1, endToday)!!
        viewModel.resultMediumSuspend(fetchedDto)

        // Ждем применения update в БД
        pollUntilTrue {
            val updated = cardDao.getNextToReview(Long.MAX_VALUE)
            updated != null && updated.status == StatusEnum.IN_PROGRESS
        }

        val updated = cardDao.getNextToReview(Long.MAX_VALUE)!!
        assertEquals(StatusEnum.IN_PROGRESS, updated.status)
        // intervalMinutes для расчета = round(1 * 2.5) = 2 дня => должно быть после конца текущего дня
        assertTrue(updated.nextReviewDate > endToday)

        // Не должна появляться в выдаче сегодня
        val nextToday = viewModel.getNextCardDtoToReview(1, endToday)
        assertNull(nextToday)
    }

    @Test
    fun resultEasy_updatesEfAndNotDueSameDay() = runBlocking {
        val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
        val dueCard = Card(
            nextReviewDate = now - 60,
            categoryId = 1,
            cardType = CardTypeEnum.TRANSLATE,
            intervalMinutes = 1440,
            status = StatusEnum.NEW,
            ef = 2.5
        )
        cardDao.insert(dueCard)

        val endToday = endOfTodayEpoch()
        val fetchedDto = viewModel.getNextCardDtoToReview(1, endToday)!!
        viewModel.resultEasySuspend(fetchedDto)

        pollUntilTrue {
            val updated = cardDao.getNextToReview(Long.MAX_VALUE)
            updated != null && updated.status == StatusEnum.IN_PROGRESS && Math.abs(updated.ef - 2.6) < 1e-9
        }

        val updated = cardDao.getNextToReview(Long.MAX_VALUE)!!
        assertEquals(StatusEnum.IN_PROGRESS, updated.status)
        // EF должен увеличиться на 0.1
        assertEquals(2.6, updated.ef, 1e-9)
        // intervalMinutes = round(1 * 2.5 * 1.5) = 4 дня => после конца текущего дня
        assertTrue(updated.nextReviewDate > endToday)

        // Не должна появляться в выдаче сегодня
        val nextToday = viewModel.getNextCardDtoToReview(1, endToday)
        assertNull(nextToday)
    }

    @Test
    fun resultHard_setsShortintervalMinutes_andLikelyDueSameDay() = runBlocking {
        val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
        val dueCard = Card(
            nextReviewDate = now - 60,
            categoryId = 1,
            cardType = CardTypeEnum.TRANSLATE,
            intervalMinutes = 1440,
            status = StatusEnum.NEW,
            ef = 2.5
        )
        cardDao.insert(dueCard)

        val endToday = endOfTodayEpoch()
        val fetchedDto = viewModel.getNextCardDtoToReview(1, endToday)!!
        viewModel.resultHardSuspend(fetchedDto)

        pollUntilTrue {
            val updated = cardDao.getNextToReview(Long.MAX_VALUE)
            updated != null && updated.status == StatusEnum.PROGRESS_RESET
        }

        val updated = cardDao.getNextToReview(Long.MAX_VALUE)!!
        assertEquals(StatusEnum.PROGRESS_RESET, updated.status)
        // После hard — 10 минут, зачастую до конца дня => попадет снова в выборку сегодня
        val nextToday = viewModel.getNextCardDtoToReview(1, endToday)
        val eng = (nextToday?.info as? com.example.tala.model.dto.info.WordCardInfo)?.english
        assertEquals("gamma", eng)
    }

    @Test
    fun resultMedium_repeatedlyIncreasesInterval() = runBlocking {
        val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
        val dueCard = Card(
            nextReviewDate = now - 60,
            categoryId = 1,
            cardType = CardTypeEnum.TRANSLATE,
            intervalMinutes = 1440,
            status = StatusEnum.NEW,
            ef = 2.5
        )
        cardDao.insert(dueCard)

        // 1-й повтор: Medium
        var endTime = endOfTodayEpoch()
        var dto = viewModel.getNextCardDtoToReview(1, endTime)!!
        viewModel.resultMediumSuspend(dto)

        pollUntilTrue {
            val updated = cardDao.getNextToReview(Long.MAX_VALUE)
            updated != null && updated.status == StatusEnum.IN_PROGRESS
        }
        var updated1 = cardDao.getNextToReview(Long.MAX_VALUE)!!
        val interval1 = updated1.intervalMinutes

        // 2-й повтор: переставляем время и снова Medium
        endTime = updated1.nextReviewDate + 1L
        dto = viewModel.getNextCardDtoToReview(1, endTime)!!
        viewModel.resultMediumSuspend(dto)

        pollUntilTrue {
            val c = cardDao.getNextToReview(Long.MAX_VALUE)
            c != null && c.intervalMinutes > interval1
        }
        var updated2 = cardDao.getNextToReview(Long.MAX_VALUE)!!
        val interval2 = updated2.intervalMinutes
        assertTrue(interval2 > interval1)

        // 3-й повтор: снова переставляем время и Medium
        endTime = updated2.nextReviewDate + 1L
        dto = viewModel.getNextCardDtoToReview(1, endTime)!!
        viewModel.resultMediumSuspend(dto)

        pollUntilTrue {
            val c = cardDao.getNextToReview(Long.MAX_VALUE)
            c != null && c.intervalMinutes > interval2
        }
        val updated3 = cardDao.getNextToReview(Long.MAX_VALUE)!!
        val interval3 = updated3.intervalMinutes
        assertTrue(interval3 > interval2)
    }
}
