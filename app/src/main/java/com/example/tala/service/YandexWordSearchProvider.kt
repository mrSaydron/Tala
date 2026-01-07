package com.example.tala.service

import com.example.tala.entity.word.Word
import com.example.tala.entity.word.WordLevel
import com.example.tala.entity.word.PartOfSpeech
import com.example.tala.entity.word.TagType
import com.example.tala.integration.dictionary.YandexDictionaryApi
import com.example.tala.integration.dictionary.dto.Definition
import com.example.tala.integration.dictionary.dto.Translation

class YandexWordSearchProvider(
    private val api: YandexDictionaryApi = ApiClient.yandexDictionaryApi,
    private val apiKey: String = YandexDictionaryApi.YANDEX_API_KEY,
    private val fallbackLevel: WordLevel? = null,
) : WordSearchProvider {

    override fun detectLanguage(term: String): WordSearchLanguage {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return WordSearchLanguage.UNKNOWN

        val hasCyrillic = trimmed.any { it in CYRILLIC_CHARS }
        val hasLatin = trimmed.any { it in LATIN_CHARS }

        return when {
            hasCyrillic && !hasLatin -> WordSearchLanguage.RUSSIAN
            hasLatin && !hasCyrillic -> WordSearchLanguage.ENGLISH
            hasLatin -> WordSearchLanguage.ENGLISH
            hasCyrillic -> WordSearchLanguage.RUSSIAN
            else -> WordSearchLanguage.UNKNOWN
        }
    }

    override suspend fun searchByRussian(term: String): List<List<Word>> {
        val normalizedQuery = term.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        val firstResponse = api.getTranslation(
            text = normalizedQuery,
            lang = RUS_TO_ENG,
            apiKey = apiKey
        )

        val firstTranslation = firstResponse.def
            .firstOrNull { it.tr.isNotEmpty() }
            ?.tr?.firstOrNull()
            ?: return emptyList()

        val englishWord = firstTranslation.text

        val secondResponse = api.getTranslation(
            text = englishWord,
            lang = ENG_TO_RUS,
            apiKey = apiKey
        )

        val dictionaries = secondResponse.def.flatMap(::mapEnglishDefinition)
            .distinctBy { it.word.lowercase() to it.translation.lowercase() }

        return wrapAsSingleGroup(dictionaries)
    }

    override suspend fun searchByEnglish(term: String): List<List<Word>> {
        if (term.isBlank()) return emptyList()
        val response = api.getTranslation(
            text = term.trim(),
            lang = ENG_TO_RUS,
            apiKey = apiKey
        )

        val dictionaries = response.def.flatMap(::mapEnglishDefinition)
            .distinctBy { it.word.lowercase() to it.translation.lowercase() }

        return wrapAsSingleGroup(dictionaries)
    }

    private fun mapEnglishDefinition(definition: Definition): List<Word> {
        if (definition.tr.isEmpty()) return emptyList()

        return definition.tr.map { translation ->
            val pos = translation.pos.ifBlank { definition.pos }
            buildWordEntry(
                word = definition.text,
                translation = translation.text,
                partOfSpeech = mapPartOfSpeech(pos),
                ipa = definition.ts,
                hint = buildHintFromTranslation(translation),
                frequency = translation.fr?.toDouble(),
                tags = emptySet(),
            )
        }
    }

    private fun mapRussianDefinition(definition: Definition): List<Word> {
        if (definition.tr.isEmpty()) return emptyList()

        return definition.tr.map { translation ->
            val pos = translation.pos.ifBlank { definition.pos }
            buildWordEntry(
                word = translation.text,
                translation = definition.text,
                partOfSpeech = mapPartOfSpeech(pos),
                ipa = null,
                hint = buildHintFromTranslation(translation),
                frequency = translation.fr?.toDouble(),
                tags = emptySet(),
            )
        }
    }

    private fun buildWordEntry(
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
        val normalized = raw?.lowercase()?.replace('-', '_') ?: return PartOfSpeech.NOUN
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
            "phrasal_verb", "phr_v", "phrv" -> PartOfSpeech.PHRASAL_VERB
            "gerund" -> PartOfSpeech.GERUND
            "proper_noun", "prop" -> PartOfSpeech.PROPER_NOUN
            "idiom" -> PartOfSpeech.IDIOM
            else -> PartOfSpeech.NOUN
        }
    }

    private fun buildHintFromTranslation(translation: Translation): String? {
        val parts = buildList {
            if (!translation.syn.isNullOrEmpty()) {
                add("Синонимы: " + translation.syn.joinToString { it.text })
            }
            if (translation.mean.isNullOrEmpty()) {
                add("Значения: " + translation.mean!!.joinToString { it.text })
            }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString("\n")
    }

    private fun wrapAsSingleGroup(words: List<Word>): List<List<Word>> {
        if (words.isEmpty()) return emptyList()
        return listOf(words)
    }

    companion object {
        private const val ENG_TO_RUS = "en-ru"
        private const val RUS_TO_ENG = "ru-en"

        private val CYRILLIC_CHARS: Set<Char> =
            ('а'..'я').toSet() + ('А'..'Я').toSet() + setOf('ё', 'Ё')
        private val LATIN_CHARS: Set<Char> =
            ('a'..'z').toSet() + ('A'..'Z').toSet()
    }
}

