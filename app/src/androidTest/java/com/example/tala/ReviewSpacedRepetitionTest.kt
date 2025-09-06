package com.example.tala

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tala.entity.card.Card
import com.example.tala.entity.card.CardDao
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.model.enums.StatusEnum
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ReviewSpacedRepetitionTest {

    private lateinit var context: Context
    private lateinit var db: TalaDatabase
    private lateinit var cardDao: CardDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = androidx.room.Room.inMemoryDatabaseBuilder(context, TalaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cardDao = db.cardDao()
        runBlocking { cardDao.deleteAll() }
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun getNextToReview_ordersByNextReviewDateAscending() = runBlocking {
        val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
        val endFindDate = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

        // Вставляем три карточки с разными временами повторения в прошлом
        val c1 = Card(
            nextReviewDate = now - 300, // раньше всех
            categoryId = 1,
            cardType = CardTypeEnum.TRANSLATE,
            interval = 1,
            status = StatusEnum.NEW,
        )
        val c2 = Card(
            nextReviewDate = now - 200,
            categoryId = 1,
            cardType = CardTypeEnum.TRANSLATE,
            interval = 1,
            status = StatusEnum.NEW,
        )
        val c3 = Card(
            nextReviewDate = now - 100,
            categoryId = 1,
            cardType = CardTypeEnum.TRANSLATE,
            interval = 1,
            status = StatusEnum.NEW,
        )

        cardDao.insert(c1)
        cardDao.insert(c2)
        cardDao.insert(c3)

        // 1. Первая должна быть apple (самая ранняя дата)
        val first = cardDao.getNextToReview(endFindDate)
        // Текстовая проверка английского теперь берется из info на уровне DTO, а не entity
        assertEquals("apple", (first?.toCardDto()?.info as? com.example.tala.model.dto.info.WordCardInfo)?.english)

        // Обновляем первую карточку в далекое будущее, чтобы исключить из отбора
        val farFuture = now + 100_000
        cardDao.update(first!!.copy(nextReviewDate = farFuture, status = StatusEnum.IN_PROGRESS))

        // 2. Теперь должна быть banana
        val second = cardDao.getNextToReview(endFindDate)
        assertEquals("banana", (second?.toCardDto()?.info as? com.example.tala.model.dto.info.WordCardInfo)?.english)

        // Исключаем и её
        cardDao.update(second!!.copy(nextReviewDate = farFuture, status = StatusEnum.IN_PROGRESS))

        // 3. Теперь должна быть cherry
        val third = cardDao.getNextToReview(endFindDate)
        assertEquals("cherry", (third?.toCardDto()?.info as? com.example.tala.model.dto.info.WordCardInfo)?.english)
    }

    @Test
    fun getNextToReview_filtersByEndDate() = runBlocking {
        val now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()

        val card = Card(
            nextReviewDate = now + 3600, // через час
            categoryId = 2,
            cardType = CardTypeEnum.TRANSLATE,
            interval = 1,
            status = StatusEnum.NEW,
        )
        cardDao.insert(card)

        // Конечная дата меньше времени повторения — карточка не должна отобраться
        val endBefore = now + 600 // через 10 минут
        val notDue = cardDao.getNextToReview(endBefore)
        assertNull(notDue)

        // Конечная дата больше времени повторения — карточка должна отобраться
        val endAfter = now + 7200 // через 2 часа
        val due = cardDao.getNextToReview(endAfter)
        assertEquals("desk", (due?.toCardDto()?.info as? com.example.tala.model.dto.info.WordCardInfo)?.english)
    }
}
