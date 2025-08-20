package com.example.tala.util

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BackgroundColorSpan

object TextDiffHighlighter {

    fun buildColoredAnswer(
        user: String,
        correct: String,
        correctBgColor: Int = Color.parseColor("#4CAF50"),
        incorrectBgColor: Int = Color.parseColor("#F44336"),
        missingBgColor: Int = Color.parseColor("#FFEB3B"),
        ignoreCase: Boolean = true
    ): SpannableStringBuilder {
        val m = user.length
        val n = correct.length

        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (user[i - 1].equals(correct[j - 1], ignoreCase = ignoreCase)) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        val aligned = ArrayList<Pair<Char?, Char?>>()
        var i = m
        var j = n
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                val cost = if (user[i - 1].equals(correct[j - 1], ignoreCase = ignoreCase)) 0 else 1
                if (dp[i][j] == dp[i - 1][j - 1] + cost) {
                    aligned.add(user[i - 1] to correct[j - 1])
                    i--; j--; continue
                }
            }
            if (i > 0 && dp[i][j] == dp[i - 1][j] + 1) {
                aligned.add(user[i - 1] to null)
                i--; continue
            }
            if (j > 0 && dp[i][j] == dp[i][j - 1] + 1) {
                aligned.add(null to correct[j - 1])
                j--; continue
            }
        }
        aligned.reverse()

        val out = SpannableStringBuilder()
        for ((u, c) in aligned) {
            if (u != null) {
                val start = out.length
                out.append(u)
                val isMatch = c != null && u.equals(c, ignoreCase = ignoreCase)
                val bgColor = if (isMatch) correctBgColor else incorrectBgColor
                out.setSpan(BackgroundColorSpan(bgColor), start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (u == null && c != null) {
                val startIns = out.length
                out.append(c)
                out.setSpan(BackgroundColorSpan(missingBgColor), startIns, startIns + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return out
    }
}


