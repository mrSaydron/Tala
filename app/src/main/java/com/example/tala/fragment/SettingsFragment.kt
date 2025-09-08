package com.example.tala.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.tala.R
import com.example.tala.ReviewSettings
import com.example.tala.databinding.FragmentSettingsBinding
import com.example.tala.entity.collection.CollectionViewModel
import com.example.tala.entity.card.CardViewModel
import com.example.tala.model.enums.CardTypeEnum
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding

    private lateinit var reviewSettings: ReviewSettings

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация настроек
        reviewSettings = ReviewSettings(requireContext())

        // Заполняем поля текущими значениями
        binding.translateEfInput.setText(reviewSettings.getEf(CardTypeEnum.TRANSLATE).toString())
        binding.reverseTranslateEfInput.setText(reviewSettings.getEf(CardTypeEnum.REVERSE_TRANSLATE).toString())
        binding.enterWordEfInput.setText(reviewSettings.getEf(CardTypeEnum.ENTER_WORD).toString())
        binding.sentenceToStudiedEfInput.setText(reviewSettings.getEf(CardTypeEnum.SENTENCE_TO_STUDIED_LANGUAGE).toString())
        binding.sentenceToStudentEfInput.setText(reviewSettings.getEf(CardTypeEnum.SENTENCE_TO_STUDENT_LANGUAGE).toString())

        // Инициализация Spinner
        val levels = resources.getStringArray(R.array.english_levels)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, levels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.levelSpinner.adapter = adapter

        // Установка текущего уровня
        val currentLevel = reviewSettings.englishLevel
        val levelIndex = levels.indexOfFirst { it.startsWith(currentLevel) }
        if (levelIndex != -1) {
            binding.levelSpinner.setSelection(levelIndex)
        }

        // Обработка нажатия на кнопку "Сохранить"
        binding.saveSettingsButton.setOnClickListener {
            saveSettings()
        }

        // Обработка нажатия на кнопку "Очистить данные"
        binding.clearDataButton.setOnClickListener {
            clearDatabase()
        }
    }

    private fun saveSettings() {
        val translateEf = binding.translateEfInput.text.toString().toDoubleOrNull() ?: CardTypeEnum.TRANSLATE.defaultEf
        val reverseTranslateEf = binding.reverseTranslateEfInput.text.toString().toDoubleOrNull() ?: CardTypeEnum.REVERSE_TRANSLATE.defaultEf
        val enterWordEf = binding.enterWordEfInput.text.toString().toDoubleOrNull() ?: CardTypeEnum.ENTER_WORD.defaultEf
        val sentenceToStudiedEf = binding.sentenceToStudiedEfInput.text.toString().toDoubleOrNull() ?: CardTypeEnum.SENTENCE_TO_STUDIED_LANGUAGE.defaultEf
        val sentenceToStudentEf = binding.sentenceToStudentEfInput.text.toString().toDoubleOrNull() ?: CardTypeEnum.SENTENCE_TO_STUDENT_LANGUAGE.defaultEf
        val englishLevel = binding.levelSpinner.selectedItem.toString().substring(0, 2)

        reviewSettings.setEf(CardTypeEnum.TRANSLATE, translateEf)
        reviewSettings.setEf(CardTypeEnum.REVERSE_TRANSLATE, reverseTranslateEf)
        reviewSettings.setEf(CardTypeEnum.ENTER_WORD, enterWordEf)
        reviewSettings.setEf(CardTypeEnum.SENTENCE_TO_STUDIED_LANGUAGE, sentenceToStudiedEf)
        reviewSettings.setEf(CardTypeEnum.SENTENCE_TO_STUDENT_LANGUAGE, sentenceToStudentEf)
        reviewSettings.englishLevel = englishLevel

        Toast.makeText(requireContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show()
    }

    private fun clearDatabase() {
        lifecycleScope.launch {
            val cardViewModel = ViewModelProvider(requireActivity())[CardViewModel::class.java]
            val categoryViewModel = ViewModelProvider(requireActivity())[CollectionViewModel::class.java]
            cardViewModel.deleteAllWords()
            categoryViewModel.deleteAllCollections()
            Toast.makeText(requireContext(), "Данные очищены", Toast.LENGTH_SHORT).show()
        }
    }
}