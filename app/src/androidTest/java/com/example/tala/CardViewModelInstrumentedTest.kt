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
            english = "alpha",
            russian = "альфа",
            nextReviewDate = now - 60, // уже просрочена
            categoryId = 1,
            cardType = CardTypeEnum.TRANSLATE,
            interval = 1,
            status = StatusEnum.NEW,
            ef = 2.5
        )
        cardDao.insert(dueCard)

        val endToday = endOfTodayEpoch()
        val fetched = viewModel.getNextWordToReview(endToday)!!
        viewModel.resultMedium(fetched)

        // Ждем применения update в БД
        pollUntilTrue {
            val updated = cardDao.getNextToReview(Long.MAX_VALUE)
            updated != null && updated.status == StatusEnum.IN_PROGRESS
        }

        val updated = cardDao.getNextToReview(Long.MAX_VALUE)!!
        assertEquals(StatusEnum.IN_PROGRESS, updated.status)
        // interval для расчета = round(1 * 2.5) = 2 дня => должно быть после конца текущего дня
        assertTrue(updated.nextReviewDate > endToday)

        // Не должна появляться в выдаче сегодня
        val nextToday = viewModel.getNextWordToReview(endToday)
        assertNull(nextToday)
    }

    @Test
    fun resultEasy_updatesEfAndNotDueSameDay() = runBlocking {
        val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
        val dueCard = Card(
            english = "beta",
            russian = "бета",
            nextReviewDate = now - 60,
            categoryId = 1,
            cardType = CardTypeEnum.TRANSLATE,
            interval = 1,
            status = StatusEnum.NEW,
            ef = 2.5
        )
        cardDao.insert(dueCard)

        val endToday = endOfTodayEpoch()
        val fetched = viewModel.getNextWordToReview(endToday)!!
        viewModel.resultEasy(fetched)

        pollUntilTrue {
            val updated = cardDao.getNextToReview(Long.MAX_VALUE)
            updated != null && updated.status == StatusEnum.IN_PROGRESS && Math.abs(updated.ef - 2.6) < 1e-9
        }

        val updated = cardDao.getNextToReview(Long.MAX_VALUE)!!
        assertEquals(StatusEnum.IN_PROGRESS, updated.status)
        // EF должен увеличиться на 0.1
        assertEquals(2.6, updated.ef, 1e-9)
        // interval = round(1 * 2.5 * 1.5) = 4 дня => после конца текущего дня
        assertTrue(updated.nextReviewDate > endToday)

        // Не должна появляться в выдаче сегодня
        val nextToday = viewModel.getNextWordToReview(endToday)
        assertNull(nextToday)
    }

    @Test
    fun resultHard_setsShortInterval_andLikelyDueSameDay() = runBlocking {
        val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
        val dueCard = Card(
            english = "gamma",
            russian = "гамма",
            nextReviewDate = now - 60,
            categoryId = 1,
            cardType = CardTypeEnum.TRANSLATE,
            interval = 1,
            status = StatusEnum.NEW,
            ef = 2.5
        )
        cardDao.insert(dueCard)

        val endToday = endOfTodayEpoch()
        val fetched = viewModel.getNextWordToReview(endToday)!!
        viewModel.resultHard(fetched)

        pollUntilTrue {
            val updated = cardDao.getNextToReview(Long.MAX_VALUE)
            updated != null && updated.status == StatusEnum.PROGRESS_RESET
        }

        val updated = cardDao.getNextToReview(Long.MAX_VALUE)!!
        assertEquals(StatusEnum.PROGRESS_RESET, updated.status)
        // После hard — 10 минут, зачастую до конца дня => попадет снова в выборку сегодня
        val nextToday = viewModel.getNextWordToReview(endToday)
        assertEquals("gamma", nextToday?.english)
    }
}
