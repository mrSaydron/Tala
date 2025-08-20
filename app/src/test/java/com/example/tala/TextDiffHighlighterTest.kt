package com.example.tala

import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import com.example.tala.util.TextDiffHighlighter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TextDiffHighlighterTest {

    companion object {
        private const val GREEN: Int = 1
        private const val RED: Int = 2
        private const val YELLOW: Int = 3
    }

    private fun spansOf(result: SpannableStringBuilder): List<BackgroundColorSpan> {
        return result.getSpans(0, result.length, BackgroundColorSpan::class.java).toList()
    }

    private fun colorToString(result: SpannableStringBuilder): String {
        val sb = StringBuilder()
        for (i in 0 until result.length) {
            val spans = result.getSpans(i, i + 1, BackgroundColorSpan::class.java)
            val color = spans.firstOrNull()?.backgroundColor ?: 0
            sb.append(
                when (color) {
                    GREEN -> "g"
                    RED -> "r"
                    YELLOW -> "y"
                    else -> "_"
                }
            )
        }
        return sb.toString()
    }

    @Test
    fun equal() {
        val user = "hello"
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct, GREEN, RED, YELLOW)
        assertEquals("hello", res.toString())
        assertEquals("ggggg", colorToString(res))
    }

    @Test
    fun oneWrongCharacter() {
        val user = "hallo" // 'a' вместо 'e'
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct, GREEN, RED, YELLOW)
        assertEquals("hallo", res.toString())
        assertEquals("grggg", colorToString(res))
    }

    @Test
    fun multipleWrongCharacters() {
        val user = "hxllz" // 'x' вместо 'e', 'z' вместо 'o'
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct, GREEN, RED, YELLOW)
        assertEquals("hxllz", res.toString())
        assertEquals("grggr", colorToString(res))
    }

    @Test
    fun missingCharacterInMiddle() {
        val user = "helo" // пропущена одна 'l'
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct, GREEN, RED, YELLOW)
        assertEquals("hello", res.toString())
        assertEquals("ggygg", colorToString(res))
    }

    @Test
    fun extraCharacterInMiddle() {
        val user = "hellxo" // лишний 'x'
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct, GREEN, RED, YELLOW)
        assertEquals("hellxo", res.toString())
        assertEquals("ggggrg", colorToString(res))
    }

    @Test
    fun missingCharacterAtStart() {
        val user = "ello" // пропущен начальный 'h'
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct, GREEN, RED, YELLOW)
        assertEquals("hello", res.toString())
        assertEquals("ygggg", colorToString(res))
    }

    @Test
    fun missingCharacterAtEnd() {
        val user = "hell" // пропущен конечный 'o'
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct, GREEN, RED, YELLOW)
        assertEquals("hello", res.toString())
        assertEquals("ggggy", colorToString(res))
    }

    @Test
    fun extraCharacterAtStart() {
        val user = "xhello" // лишний в начале
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct, GREEN, RED, YELLOW)
        assertEquals("xhello", res.toString())
        assertEquals("rggggg", colorToString(res))
    }

    @Test
    fun extraCharacterAtEnd() {
        val user = "hellox" // лишний в конце
        val correct = "hello"
        val res = TextDiffHighlighter.buildColoredAnswer(user, correct, GREEN, RED, YELLOW)
        assertEquals("hellox", res.toString())
        assertEquals("gggggr", colorToString(res))
    }

}


