package com.example.tala.service

import android.util.Log
import com.example.tala.entity.word.Word
import com.example.tala.entity.word.DictionaryLevel
import com.example.tala.entity.word.PartOfSpeech
import com.example.tala.entity.word.TagType
import com.example.tala.integration.mistral.MistralApi
import com.example.tala.integration.mistral.MistralRequest
import com.example.tala.integration.mistral.MistralRequestMessage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.Locale

class MistralDictionarySearchProvider(
    private val api: MistralApi = ApiClient.mistralApi,
    private val apiKey: String = MistralApi.MISTRAL_API_KEY,
    private val model: String = DEFAULT_MODEL,
    private val fallbackLevel: DictionaryLevel? = null,
    private val fallbackProvider: DictionarySearchProvider? = null,
    private val gson: Gson = Gson(),
) : DictionarySearchProvider {

    override fun detectLanguage(term: String): DictionarySearchLanguage {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return DictionarySearchLanguage.UNKNOWN

        val hasCyrillic = trimmed.any { it in CYRILLIC_CHARS }
        val hasLatin = trimmed.any { it in LATIN_CHARS }

        return when {
            hasCyrillic && !hasLatin -> DictionarySearchLanguage.RUSSIAN
            hasLatin && !hasCyrillic -> DictionarySearchLanguage.ENGLISH
            hasLatin -> DictionarySearchLanguage.ENGLISH
            hasCyrillic -> DictionarySearchLanguage.RUSSIAN
            else -> DictionarySearchLanguage.UNKNOWN
        }
    }

    override suspend fun searchByRussian(term: String): List<List<Word>> {
        val normalized = term.trim()
        if (normalized.isEmpty()) return emptyList()
        return fetchWithFallback(normalized, DictionarySearchLanguage.RUSSIAN)
    }

    override suspend fun searchByEnglish(term: String): List<List<Word>> {
        val normalized = term.trim()
        if (normalized.isEmpty()) return emptyList()
        return fetchWithFallback(normalized, DictionarySearchLanguage.ENGLISH)
    }

    private suspend fun fetchWithFallback(
        term: String,
        language: DictionarySearchLanguage,
    ): List<List<Word>> {
        val mistralResult = runCatching { requestEntries(term, language) }
            .onFailure { error ->
                Log.e(TAG, "Mistral request failed: ${error.message}", error)
            }
            .getOrElse { emptyList() }

        if (mistralResult.isNotEmpty()) {
            return wrapAsSingleGroup(mistralResult)
        }

        val fallback = fallbackProvider ?: return emptyList()
        return when (language) {
            DictionarySearchLanguage.RUSSIAN -> fallback.searchByRussian(term)
            DictionarySearchLanguage.ENGLISH, DictionarySearchLanguage.UNKNOWN -> fallback.searchByEnglish(term)
        }
    }

    private suspend fun requestEntries(
        term: String,
        language: DictionarySearchLanguage,
    ): List<Word> {
        val response = api.generateText(
            apiKey = apiKey,
            request = MistralRequest(
                model = model,
                messages = listOf(
                    MistralRequestMessage(
                        role = "system",
                        content = SYSTEM_PROMPT
                    ),
                    MistralRequestMessage(
                        role = "user",
                        content = buildPrompt(term, language)
                    )
                )
            )
        )

        val rawContent = response.choices.firstOrNull()?.message?.content?.trim().orEmpty()
        if (rawContent.isEmpty()) return emptyList()

        val parsed = parseResponse(rawContent) ?: return emptyList()
        if (parsed.entries.isEmpty()) return emptyList()

        val dictionaries = mapEntries(parsed)
        return dictionaries
            .filter { it.word.isNotBlank() && it.translation.isNotBlank() }
            .distinctBy { (it.word.lowercase(Locale.ROOT) to it.translation.lowercase(Locale.ROOT)) }
    }

    private fun parseResponse(raw: String): LlmDictionaryResponse? {
        val sanitized = sanitizeJson(raw)
        if (sanitized.isBlank()) return null

        return runCatching { gson.fromJson(sanitized, LlmDictionaryResponse::class.java) }
            .getOrElse { error ->
                if (error !is JsonSyntaxException) return null
                val fallbackJson = extractJsonObject(sanitized)
                if (fallbackJson == null) {
                    Log.e(TAG, "Unable to parse Mistral JSON: ${error.message}")
                    return null
                }
                runCatching { gson.fromJson(fallbackJson, LlmDictionaryResponse::class.java) }
                    .onFailure { inner ->
                        Log.e(TAG, "Fallback JSON parse failed: ${inner.message}")
                    }
                    .getOrNull()
            }
    }

    private fun sanitizeJson(raw: String): String {
        var result = raw.trim()
        if (result.startsWith("```")) {
            result = result.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
        }
        if (result.endsWith("```")) {
            result = result.removeSuffix("```")
        }
        return result.trim()
    }

    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || start >= end) return null
        return raw.substring(start, end + 1)
    }

    private fun mapEntries(response: LlmDictionaryResponse): List<Word> {
        val result = mutableListOf<Word>()
        response.entries.forEach { entry ->
            val word = entry.word?.takeIf { it.isNotBlank() } ?: return@forEach
            val translation = entry.translation?.takeIf { it.isNotBlank() } ?: return@forEach

            val baseTags = mapTags(entry.tags)
            val baseHint = mergeHints(
                entry.hint,
                entry.notes,
                listToHint("Исключения", entry.exceptions),
                listToHint("Примеры", entry.examples)
            )

            result += buildDictionaryEntry(
                word = word,
                translation = translation,
                partOfSpeech = mapPartOfSpeech(entry.partOfSpeech),
                ipa = entry.ipa,
                hint = baseHint,
                frequency = entry.frequency,
                tags = baseTags,
            )

            entry.forms.orEmpty().forEach { form ->
                val formWord = form.word?.takeIf { it.isNotBlank() } ?: return@forEach
                val formTranslation = form.translation?.takeIf { it.isNotBlank() } ?: translation

                val formTags = (mapTags(form.tags) + listOfNotNull(mapTag(form.label))).toSet()
                val formHint = mergeHints(form.hint, form.notes)

                result += buildDictionaryEntry(
                    word = formWord,
                    translation = formTranslation,
                    partOfSpeech = mapPartOfSpeech(form.partOfSpeech ?: entry.partOfSpeech),
                    ipa = null,
                    hint = formHint,
                    frequency = null,
                    tags = formTags,
                )
            }

            entry.derived.orEmpty().forEach { derived ->
                val derivedWord = derived.word?.takeIf { it.isNotBlank() } ?: return@forEach
                val derivedTranslation = derived.translation?.takeIf { it.isNotBlank() } ?: translation
                val derivedHint = mergeHints(
                    derived.hint,
                    derived.notes,
                    listToHint("Примеры", derived.examples)
                )

                result += buildDictionaryEntry(
                    word = derivedWord,
                    translation = derivedTranslation,
                    partOfSpeech = mapPartOfSpeech(derived.partOfSpeech),
                    ipa = null,
                    hint = derivedHint,
                    frequency = null,
                    tags = mapTags(derived.tags),
                )
            }
        }
        return result
    }

    private fun buildPrompt(term: String, language: DictionarySearchLanguage): String {
        val (inputLanguage, targetLanguage) = when (language) {
            DictionarySearchLanguage.ENGLISH -> "English" to "Russian"
            DictionarySearchLanguage.RUSSIAN -> "Russian" to "English"
            DictionarySearchLanguage.UNKNOWN -> "English" to "Russian"
        }

        return """
            Term: "$term".
            Input language: $inputLanguage.
            Target language: $targetLanguage.
            Provide up to $MAX_ENTRIES dictionary entries for this term. Include irregular forms, comparative/superlative, verb tenses, noun/verb derivations, phrasal expressions, and noteworthy collocations when relevant.
            Respond strictly in JSON matching this schema:
            {
              "entries": [
                {
                  "word": "base word in $inputLanguage",
                  "translation": "translation in $targetLanguage",
                  "partOfSpeech": "noun|verb|adjective|adverb|pronoun|preposition|conjunction|interjection|determiner|article|numeral|particle|auxiliary_verb|modal_verb|phrasal_verb|gerund|proper_noun|idiom",
                  "ipa": "International Phonetic Alphabet transcription if available",
                  "hint": "short usage hint or memory trick",
                  "notes": "important notes such as register or irregularities",
                  "frequency": 0.0,
                  "tags": ["comparative", "superlative", "past_simple", "past_participle", "plural", "is_phrasal", "is_idiom", "is_collocation", "is_fixed_expression", "case_sensitive"],
                  "examples": ["example sentence in $inputLanguage with translation"],
                  "exceptions": ["list of irregular patterns"],
                  "forms": [
                    {
                      "word": "inflected word in $inputLanguage",
                      "translation": "translation in $targetLanguage",
                      "partOfSpeech": "same options as above",
                      "label": "past_simple|past_participle|comparative|superlative|plural|gerund|third_person|present_participle|other",
                      "hint": "usage hint",
                      "notes": "additional notes",
                      "tags": ["comparative", "superlative", "past_simple", "past_participle", "plural"]
                    }
                  ],
                  "derived": [
                    {
                      "word": "derived word in $inputLanguage",
                      "translation": "translation in $targetLanguage",
                      "partOfSpeech": "same options",
                      "hint": "usage hint",
                      "notes": "additional notes",
                      "tags": ["is_phrasal", "is_idiom", "is_collocation"],
                      "examples": ["short example"]
                    }
                  ]
                }
              ]
            }
            Use null for fields you cannot fill. Do not include Markdown or explanatory text outside the JSON object.
        """.trimIndent()
    }

    private fun buildDictionaryEntry(
        word: String,
        translation: String,
        partOfSpeech: PartOfSpeech,
        ipa: String?,
        hint: String?,
        frequency: Double?,
        tags: Set<TagType>,
    ): Word {
        return Word(
            word = word,
            translation = translation,
            partOfSpeech = partOfSpeech,
            ipa = ipa,
            hint = hint,
            baseWordId = null,
            frequency = frequency,
            level = fallbackLevel,
            tags = tags,
        )
    }

    private fun mapPartOfSpeech(raw: String?): PartOfSpeech {
        val normalized = raw?.trim()?.lowercase(Locale.ROOT)?.replace('-', '_') ?: return PartOfSpeech.UNKNOWN
        return when (normalized) {
            "noun", "s", "n" -> PartOfSpeech.NOUN
            "verb", "v" -> PartOfSpeech.VERB
            "adjective", "adj" -> PartOfSpeech.ADJECTIVE
            "adverb", "adv" -> PartOfSpeech.ADVERB
            "pronoun", "pron" -> PartOfSpeech.PRONOUN
            "preposition", "prep" -> PartOfSpeech.PREPOSITION
            "conjunction", "conj" -> PartOfSpeech.CONJUNCTION
            "interjection", "int", "interj" -> PartOfSpeech.INTERJECTION
            "determiner", "det" -> PartOfSpeech.DETERMINER
            "article" -> PartOfSpeech.ARTICLE
            "numeral", "num" -> PartOfSpeech.NUMERAL
            "particle", "part" -> PartOfSpeech.PARTICLE
            "auxiliary_verb", "aux" -> PartOfSpeech.AUXILIARY_VERB
            "modal_verb", "modal" -> PartOfSpeech.MODAL_VERB
            "phrasal_verb", "phrv", "phr_v" -> PartOfSpeech.PHRASAL_VERB
            "gerund" -> PartOfSpeech.GERUND
            "proper_noun", "prop" -> PartOfSpeech.PROPER_NOUN
            "idiom" -> PartOfSpeech.IDIOM
            "other" -> PartOfSpeech.UNKNOWN
            else -> PartOfSpeech.UNKNOWN
        }
    }

    private fun mapTags(rawTags: List<String>?): Set<TagType> {
        if (rawTags.isNullOrEmpty()) return emptySet()
        val tags = rawTags.mapNotNull { mapTag(it) }
        return tags.toSet()
    }

    private fun mapTag(raw: String?): TagType? {
        val normalized = raw?.trim()?.lowercase(Locale.ROOT)?.replace('-', '_') ?: return null
        return when (normalized) {
            "comparative" -> TagType.COMPARATIVE
            "superlative" -> TagType.SUPERLATIVE
            "plural" -> TagType.PLURAL
            "past_simple" -> TagType.PAST_SIMPLE
            "past_participle" -> TagType.PAST_PARTICIPLE
            "is_idiom" -> TagType.IS_IDIOM
            "is_phrasal" -> TagType.IS_PHRASAL
            "is_collocation" -> TagType.IS_COLLOCATION
            "is_fixed_expression" -> TagType.IS_FIXED_EXPRESSION
            "case_sensitive" -> TagType.CASE_SENSITIVE
            else -> null
        }
    }

    private fun wrapAsSingleGroup(dictionaries: List<Word>): List<List<Word>> {
        if (dictionaries.isEmpty()) return emptyList()
        return listOf(dictionaries)
    }

    private fun mergeHints(vararg parts: String?): String? {
        val items = parts.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
        return items.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n")
    }

    private fun listToHint(title: String, values: List<String>?): String? {
        if (values.isNullOrEmpty()) return null
        val content = values.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        if (content.isEmpty()) return null
        return "$title: ${content.joinToString(separator = "; ")}"
    }

    data class LlmDictionaryResponse(
        val entries: List<LlmDictionaryEntry> = emptyList(),
    )

    data class LlmDictionaryEntry(
        val word: String?,
        val translation: String?,
        val partOfSpeech: String? = null,
        val ipa: String? = null,
        val hint: String? = null,
        val notes: String? = null,
        val frequency: Double? = null,
        val tags: List<String>? = null,
        val examples: List<String>? = null,
        val exceptions: List<String>? = null,
        val forms: List<LlmDictionaryForm>? = null,
        val derived: List<LlmDictionaryDerived>? = null,
    )

    data class LlmDictionaryForm(
        val word: String?,
        val translation: String? = null,
        val partOfSpeech: String? = null,
        val label: String? = null,
        val hint: String? = null,
        val notes: String? = null,
        val tags: List<String>? = null,
    )

    data class LlmDictionaryDerived(
        val word: String?,
        val translation: String?,
        val partOfSpeech: String? = null,
        val hint: String? = null,
        val notes: String? = null,
        val tags: List<String>? = null,
        val examples: List<String>? = null,
    )

    companion object {
        private const val TAG = "MistralWord"
        private const val DEFAULT_MODEL = "mistral-small-latest"
        private const val MAX_ENTRIES = 15

        private val CYRILLIC_CHARS: Set<Char> =
            ('а'..'я').toSet() + ('А'..'Я').toSet() + setOf('ё', 'Ё')
        private val LATIN_CHARS: Set<Char> =
            ('a'..'z').toSet() + ('A'..'Z').toSet()

        private const val SYSTEM_PROMPT = """
            You are an assistant that prepares structured bilingual dictionary entries for language learners.
            Always respond with a single JSON object matching the provided schema. Do not add markdown fences, explanations, or extra keys.
        """
    }
}


