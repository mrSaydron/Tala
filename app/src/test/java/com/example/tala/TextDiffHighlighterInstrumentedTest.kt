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
}


