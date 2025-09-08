package com.example.tala.fragment

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.example.tala.R
import com.example.tala.databinding.FragmentWordListBinding
import com.example.tala.entity.collection.CardCollection
import com.example.tala.entity.collection.CollectionViewModel
import com.example.tala.entity.card.CardViewModel
import com.example.tala.fragment.adapter.CardAdapter
import com.example.tala.model.dto.CardListDto

class WordListFragment : Fragment() {

    private lateinit var binding: FragmentWordListBinding

    private lateinit var cardViewModel: CardViewModel
    private lateinit var categoryViewModel: CollectionViewModel

    private lateinit var categoryAdapter: ArrayAdapter<String>
    private val categories = mutableListOf<CardCollection>()

    // Сохранение состояния фильтра и списка при возврате к фрагменту
    private var selectedCategoryPosition: Int = 0
    private var recyclerViewState: Parcelable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWordListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация ViewModel
        cardViewModel = ViewModelProvider(requireActivity())[CardViewModel::class.java]
        categoryViewModel = ViewModelProvider(requireActivity())[CollectionViewModel::class.java]

        // Инициализация Spinner
        categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        categoryViewModel.getAllCollections().observe(viewLifecycleOwner) { categoryList ->
            categories.clear()
            categories.add(CardCollection(id = 0, name = "Все"))
            categories.addAll(categoryList)
            categoryAdapter.clear()
            categoryAdapter.addAll(categories.map { it.name })
            categoryAdapter.notifyDataSetChanged()

            // Восстанавливаем выбранную категорию после пересоздания вью
            if (selectedCategoryPosition in 0 until categories.size) {
                binding.categorySpinner.setSelection(selectedCategoryPosition, false)
            } else {
                binding.categorySpinner.setSelection(0, false)
            }
        }

        // Обработка выбора категории
        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in categories.indices) {
                    selectedCategoryPosition = position
                    val selectedCategory = categories[position]
                    loadWordsByCategory(selectedCategory.id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Ничего не делаем
            }
        }

        // Переход к добавлению слов
        binding.addWordsButton.setOnClickListener {
            // Сохраняем позицию прокрутки перед переходом
            recyclerViewState = binding.wordRecyclerView.layoutManager?.onSaveInstanceState()
            val addWordFragment = AddWordFragment()
            replaceFragment(addWordFragment)
        }
    }

    private fun loadWordsByCategory(categoryId: Int) {
        val wordsLiveData: LiveData<List<CardListDto>> = if (categoryId == 0) {
            cardViewModel.allCardList()
        } else {
            cardViewModel.getCardListByCategory(categoryId)
        }

        wordsLiveData.observe(viewLifecycleOwner) { words ->
            val categoryIdToName = categories.associate { it.id to it.name }
            val adapter = CardAdapter(
                words,
                onItemClick = { word ->
                    // Сохраняем позицию прокрутки перед переходом к редактированию
                    recyclerViewState = binding.wordRecyclerView.layoutManager?.onSaveInstanceState()
                    val editFragment = AddWordFragment.newInstance(word)
                    replaceFragment(editFragment)
                },
                categoryIdToName = categoryIdToName
            )
            binding.wordRecyclerView.adapter = adapter

            // Восстанавливаем позицию прокрутки списка, если есть сохранённая
            recyclerViewState?.let { state ->
                binding.wordRecyclerView.layoutManager?.onRestoreInstanceState(state)
                recyclerViewState = null
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Сохраняем текущую выбранную категорию и позицию прокрутки при уничтожении вью
        selectedCategoryPosition = binding.categorySpinner.selectedItemPosition
        recyclerViewState = binding.wordRecyclerView.layoutManager?.onSaveInstanceState()
    }
}