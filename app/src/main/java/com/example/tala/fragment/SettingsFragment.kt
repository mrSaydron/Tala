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
import com.example.tala.entity.category.CategoryViewModel
import com.example.tala.entity.card.CardViewModel
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
        binding.easyIntervalInput.setText(reviewSettings.easyInterval.toString())
        binding.mediumIntervalInput.setText(reviewSettings.mediumInterval.toString())
        binding.hardIntervalInput.setText(reviewSettings.hardInterval.toString())

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
        val easyInterval = binding.easyIntervalInput.text.toString().toIntOrNull() ?: 4
        val mediumInterval = binding.mediumIntervalInput.text.toString().toIntOrNull() ?: 2
        val hardInterval = binding.hardIntervalInput.text.toString().toIntOrNull() ?: 1
        val englishLevel = binding.levelSpinner.selectedItem.toString().substring(0, 2)

        reviewSettings.easyInterval = easyInterval
        reviewSettings.mediumInterval = mediumInterval
        reviewSettings.hardInterval = hardInterval
        reviewSettings.englishLevel = englishLevel

        Toast.makeText(requireContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show()
    }

    private fun clearDatabase() {
        lifecycleScope.launch {
            val cardViewModel = ViewModelProvider(requireActivity())[CardViewModel::class.java]
            val categoryViewModel = ViewModelProvider(requireActivity())[CategoryViewModel::class.java]
            cardViewModel.deleteAllWords()
            categoryViewModel.deleteAllCategories()
            Toast.makeText(requireContext(), "Данные очищены", Toast.LENGTH_SHORT).show()
        }
    }
}