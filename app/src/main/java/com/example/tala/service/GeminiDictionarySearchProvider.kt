package com.example.tala.service

import android.util.Log
import com.example.tala.entity.dictionary.Dictionary
import com.example.tala.entity.dictionary.PartOfSpeech
import com.example.tala.entity.dictionary.TagType
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.util.Locale

class GeminiDictionarySearchProvider(
    private val modelName: String = DEFAULT_MODEL,
    private val fallbackProvider: DictionarySearchProvider? = null,
    private val temperature: Float = DEFAULT_TEMPERATURE,
    private val gson: Gson = Gson(),
) : DictionarySearchProvider {

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                this.temperature = this@GeminiDictionarySearchProvider.temperature
                responseMimeType = APPLICATION_JSON
                responseSchema = RESPONSE_SCHEMA
            },
            systemInstruction = content(role = "system") {
                text(SYSTEM_PROMPT)
            }
        )

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

    override suspend fun searchByRussian(term: String): List<List<Dictionary>> {
        val normalized = term.trim()
        if (normalized.isEmpty()) return emptyList()
        return fetchWithFallback(normalized, DictionarySearchLanguage.RUSSIAN)
    }

    override suspend fun searchByEnglish(term: String): List<List<Dictionary>> {
        val normalized = term.trim()
        if (normalized.isEmpty()) return emptyList()
        return fetchWithFallback(normalized, DictionarySearchLanguage.ENGLISH)
    }

    private suspend fun fetchWithFallback(
        term: String,
        language: DictionarySearchLanguage,
    ): List<List<Dictionary>> {
        val geminiResult = runCatching { requestEntries(term, language) }
            .onFailure { error ->
                Log.e(TAG, "Gemini request failed: ${error.message}", error)
            }
            .getOrElse { emptyList() }

        if (geminiResult.any { it.isNotEmpty() }) {
            return geminiResult.filter { it.isNotEmpty() }
        }

        val fallback = fallbackProvider ?: return emptyList()
        return when (language) {
            DictionarySearchLanguage.RUSSIAN -> fallback.searchByRussian(term)
            DictionarySearchLanguage.ENGLISH,
            DictionarySearchLanguage.UNKNOWN -> fallback.searchByEnglish(term)
        }
    }

    private suspend fun requestEntries(
        term: String,
        language: DictionarySearchLanguage,
    ): List<List<Dictionary>> {
        val response = model.generateContent(buildPrompt(term, language))

        val rawContent = response.text
        if (rawContent.isNullOrEmpty()) return emptyList()

        val parsed = parseResponse(rawContent) ?: return emptyList()
        if (parsed.entries.isEmpty()) return emptyList()

        val dictionaries = mapEntries(parsed)
            .filter { it.word.isNotBlank() && it.translation.isNotBlank() }
            .distinctBy { it.word.lowercase(Locale.ROOT) to it.translation.lowercase(Locale.ROOT) }

        return groupByBaseWord(dictionaries)
    }

    private fun parseResponse(raw: String): GeminiDictionaryResponse? {
        val sanitized = sanitizeJson(raw)
        if (sanitized.isBlank()) return null

        return runCatching { gson.fromJson(sanitized, GeminiDictionaryResponse::class.java) }
            .recoverCatching { error ->
                if (error !is JsonSyntaxException) throw error
                val fallbackJson = extractJsonObject(sanitized)
                if (fallbackJson == null) {
                    Log.e(TAG, "Unable to parse Gemini JSON: ${error.message}")
                    null
                } else {
                    gson.fromJson(fallbackJson, GeminiDictionaryResponse::class.java)
                }
            }
            .onFailure { parseError ->
                Log.e(TAG, "Gemini JSON parse failed: ${parseError.message}")
            }
            .getOrNull()
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

    private fun mapEntries(response: GeminiDictionaryResponse): List<Dictionary> {
        val results = mutableListOf<Dictionary>()

        response.entries.forEach { entry ->
            val word = entry.word?.takeIf { it.isNotBlank() } ?: return@forEach
            val translation = entry.translation?.takeIf { it.isNotBlank() } ?: return@forEach

            val entryId = entry.id?.hashCode()
            val baseId = entry.baseWordId?.hashCode() ?: entryId

            results += buildDictionaryEntry(
                id = entryId,
                baseWordId = baseId,
                word = word,
                translation = translation,
                partOfSpeech = mapPartOfSpeech(entry.partOfSpeech),
                ipa = entry.ipa,
                hint = entry.hint,
                frequency = null,
                tags = mapTags(entry.tags),
                level = entry.level,
            )
        }

        return results
    }

    private fun groupByBaseWord(entries: List<Dictionary>): List<List<Dictionary>> {
        if (entries.isEmpty()) return emptyList()

        val grouped = entries.groupBy { dictionary ->
            dictionary.baseWordId
                ?: dictionary.id.takeIf { it != 0 }
                ?: (dictionary.word.lowercase(Locale.ROOT) + "|" + dictionary.translation.lowercase(Locale.ROOT)).hashCode()
        }

        return grouped.values.map { group ->
            group.sortedBy { it.id }
        }
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

            Provide dictionary entries for this term. Entries should contain:

            one or more translation options;
            irregular verb forms;
            comparative and superlative forms;
            exceptions for plural formation;
            derivatives for various parts of speech;
            phrasal expressions;
            idioms;
            collocations.

            Group words similar in meaning or sense using id and baseWordId, referring to the base word. Words that fall into different parts of speech but are similar in meaning should be grouped together. For the base word, id and baseWordId should be equal.

            Respond strictly in JSON format, following this schema:

            {
              "entries": [
                {
                  "id": "ordinal identifier",
                  "word": "base word in $inputLanguage",
                  "translation": "translation in $targetLanguage, only one value",
                  "baseWordId": "base catch identifier",
                  "partOfSpeech": "noun|verb|adjective|adverb|pronoun|preposition|conjunction|interjection|determiner|article|numeral|particle|auxiliary_verb|modal_verb|phrasal_verb|gerund|proper_noun|idiom",
                  "ipa": "International Phonetic Alphabet transcription if available",
                  "hint": "short usage hint or memory trick",
                  "tags": ["comparative", "superlative", "past_simple", "past_participle", "plural", "is_phrasal", "is_idiom", "is_collocation", "is_fixed_expression", "case_sensitive"]
                }
              ]
            }

            Use null for fields you cannot fill. Do not include Markdown or explanatory text outside the JSON object.
        """.trimIndent()
    }

    private fun buildDictionaryEntry(
        id: Int?,
        baseWordId: Int?,
        word: String,
        translation: String,
        partOfSpeech: PartOfSpeech,
        ipa: String?,
        hint: String?,
        frequency: Double?,
        tags: Set<TagType>,
        level: String?,
    ): Dictionary {
        return Dictionary(
            id = id ?: 0,
            word = word,
            translation = translation,
            partOfSpeech = partOfSpeech,
            ipa = ipa,
            hint = hint,
            baseWordId = baseWordId,
            frequency = frequency,
            level = level?.let { runCatching { com.example.tala.entity.dictionary.DictionaryLevel.valueOf(it) }.getOrNull() },
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
            else -> PartOfSpeech.UNKNOWN
        }
    }

    private fun mapTags(rawTags: List<String>?): Set<TagType> {
        if (rawTags.isNullOrEmpty()) return emptySet()
        return rawTags.mapNotNull { mapTag(it) }.toSet()
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

    data class GeminiDictionaryResponse(
        val entries: List<GeminiDictionaryEntry> = emptyList(),
    )

    data class GeminiDictionaryEntry(
        val id: String? = null,
        val word: String? = null,
        val translation: String? = null,
        val baseWordId: String? = null,
        val partOfSpeech: String? = null,
        val ipa: String? = null,
        val hint: String? = null,
        val tags: List<String>? = null,
        val level: String? = null,
    )

    companion object {
        private const val TAG = "GeminiDictionary"
        private const val DEFAULT_MODEL = "gemini-2.5-pro"
        private const val DEFAULT_TEMPERATURE = 0.2f
        private const val APPLICATION_JSON = "application/json"
        private const val SYSTEM_PROMPT = """
            You are an assistant that prepares structured bilingual dictionary entries for language learners.
            Always respond with a single JSON object matching the provided schema. Do not add markdown fences, explanations, or extra keys.
        """

        private val CYRILLIC_CHARS: Set<Char> =
            ('а'..'я').toSet() + ('А'..'Я').toSet() + setOf('ё', 'Ё')
        private val LATIN_CHARS: Set<Char> =
            ('a'..'z').toSet() + ('A'..'Z').toSet()

        private val RESPONSE_SCHEMA: Schema = Schema.obj(
            properties = mapOf(
                "entries" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "id" to Schema.string(nullable = true),
                            "word" to Schema.string(description = "Base word in input language"),
                            "translation" to Schema.string(description = "Translation in target language"),
                            "baseWordId" to Schema.string(nullable = true),
                            "partOfSpeech" to Schema.enumeration(
                                values = PartOfSpeech.entries.map { it.value },
                                description = "Part of speech",
                                nullable = true
                            ),
                            "ipa" to Schema.string(nullable = true),
                            "hint" to Schema.string(nullable = true),
                            "tags" to Schema.array(
                                items = Schema.enumeration(TagType.entries.map { it.value }),
                                nullable = true
                            ),
                            "level" to Schema.enumeration(
                                values = com.example.tala.entity.dictionary.DictionaryLevel.values().map { it.name },
                                nullable = true
                            )
                        ),
                        optionalProperties = listOf("id", "baseWordId", "partOfSpeech", "ipa", "hint", "tags"),
                        description = "Dictionary entry"
                    ),
                    description = "List of dictionary entries",
                    minItems = 1
                )
            ),
            optionalProperties = emptyList(),
            description = "Gemini dictionary response"
        )
    }
}