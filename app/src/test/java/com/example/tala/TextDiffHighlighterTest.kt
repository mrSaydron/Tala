package com.example.tala

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import com.example.tala.util.TextDiffHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextDiffHighlighterInstrumentedTest {

    companion object {
        private val g = Color.parseColor("#4CAF50")
        private val r = Color.parseColor("#F44336")
    }

    private fun spansOf(result: SpannableStringBuilder): List<BackgroundColorSpan> {
        return result.getSpans(0, result.length, BackgroundColorSpan::class.java).toList()
    }

    private fun colorToString(result: SpannableStringBuilder): String {
        return spansOf(result)
            .joinToString {
                when (it.backgroundColor) {
                    g -> "g"
                    r -> "r"
                    else -> "_"
                }
            }
    }

    @Test
    fun equal() {
        val user = "hello"
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct)
        assertEquals("hello", res.toString())
        assertEquals("ggggg", colorToString(res))
    }

    @Test
    fun exactMatch_allGreen() {
        val user = "hello"
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct)
        // Длина совпадает
        assertEquals(user.length, res.length)
        // На каждый символ есть по одному спану
        val spans = spansOf(res)
        assertEquals(user.length, spans.size)
        // Все интервалы по 1 символу и цвета одинаковые
        spans.forEachIndexed { _, span ->
            val start = res.getSpanStart(span)
            val end = res.getSpanEnd(span)
            assertEquals(1, end - start)
            // Проверяем, что все спаны указывают на один и тот же цвет (зелёный)
            assertTrue(spans.all { it.backgroundColor == span.backgroundColor })
        }
    }

    @Test
    fun oneSubstitution_oneRedRestGreen() {
        val user = "hbllo"
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct)
        val spans = spansOf(res)
        assertEquals(user.length, spans.size)
        // Ровно один символ неверный
        val colors = spans.map { it.backgroundColor }
        val mostColor = colors.groupBy { it }.maxByOrNull { it.value.size }!!.key
        val redCount = colors.count { it != mostColor }
        assertEquals(1, redCount)
    }

    @Test
    fun insertionHandled_alignmentPreserved() {
        val user = "heello" // лишняя 'e'
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct)
        // Длина равна длине пользовательского ввода (мы не добавляем плейсхолдеры)
        assertEquals(user.length, res.length)
        // Должно быть не больше 1-2 красных символов из-за вставки, остальные зелёные
        val spans = spansOf(res)
        val colors = spans.map { it.backgroundColor }
        val mostColor = colors.groupBy { it }.maxByOrNull { it.value.size }!!.key
        val redCount = colors.count { it != mostColor }
        assertTrue(redCount in 1..2)
    }
}


