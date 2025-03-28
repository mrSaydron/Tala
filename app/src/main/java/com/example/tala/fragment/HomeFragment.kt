package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tala.R
import com.example.tala.databinding.FragmentHomeBinding
import com.example.tala.entity.category.Category
import com.example.tala.entity.category.CategoryViewModel

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private lateinit var categoryAdapter: ArrayAdapter<String>
    private val categories = mutableListOf<Category>()
    private var selectedCategoryId: Int = 0 // ID выбранной категории

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация Spinner
        categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        // Загрузка категорий
        loadCategories()

        // Обработка выбора категории
        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = categories[position].id
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategoryId = 0
            }
        }

        // Переход к обучению
        binding.startLearningButton.setOnClickListener {
            if (selectedCategoryId != 0) {
                val reviewFragment = ReviewFragment.newInstance(selectedCategoryId)
                replaceFragment(reviewFragment)
            } else {
                Toast.makeText(requireContext(), "Выберите категорию!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.settingsButton.setOnClickListener {
            val settingsFragment = SettingsFragment()
            replaceFragment(settingsFragment)
        }

        binding.wordListButton.setOnClickListener {
            val wordListFragment = WordListFragment()
            replaceFragment(wordListFragment)
        }
    }

    // Загрузка категорий
    private fun loadCategories() {
        val viewModel = ViewModelProvider(requireActivity())[CategoryViewModel::class.java]
        viewModel.getAllCategories().observe(viewLifecycleOwner) { categoryList ->
            categories.clear()
            categories.addAll(categoryList)
            categoryAdapter.clear()
            categoryAdapter.addAll(categories.map { it.name })
            categoryAdapter.notifyDataSetChanged()
        }
    }

    // Замена фрагмента
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}