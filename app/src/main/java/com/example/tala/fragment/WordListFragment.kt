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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private lateinit var collectionViewModel: CollectionViewModel

    private lateinit var collectionAdapter: ArrayAdapter<String>
    private val collections = mutableListOf<CardCollection>()

    // Сохранение состояния фильтра и списка при возврате к фрагменту
    private var selectedCollectionPosition: Int = 0
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
        collectionViewModel = ViewModelProvider(requireActivity())[CollectionViewModel::class.java]

        // Обеспечиваем отступ под статус-бар для корневого контейнера
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(v.paddingLeft, top, v.paddingRight, v.paddingBottom)
            insets
        }

        // Инициализация Spinner
        collectionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf())
        collectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.collectionSpinner.adapter = collectionAdapter

        collectionViewModel.getAllCollections().observe(viewLifecycleOwner) { collectionList ->
            collections.clear()
            collections.add(CardCollection(id = 0, name = "Все"))
            collections.addAll(collectionList)
            collectionAdapter.clear()
            collectionAdapter.addAll(collections.map { it.name })
            collectionAdapter.notifyDataSetChanged()

            // Восстанавливаем выбранную категорию после пересоздания вью
            if (selectedCollectionPosition in 0 until collections.size) {
                binding.collectionSpinner.setSelection(selectedCollectionPosition, false)
            } else {
                binding.collectionSpinner.setSelection(0, false)
            }
        }

        // Обработка выбора категории
        binding.collectionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in collections.indices) {
                    selectedCollectionPosition = position
                    val selectedCollection = collections[position]
                    loadWordsByCollection(selectedCollection.id)
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

    private fun loadWordsByCollection(collectionId: Int) {
        val wordsLiveData: LiveData<List<CardListDto>> = if (collectionId == 0) {
            cardViewModel.allCardList()
        } else {
            cardViewModel.getCardListByCollection(collectionId)
        }

        wordsLiveData.observe(viewLifecycleOwner) { words ->
            val collectionIdToName = collections.associate { it.id to it.name }
            val adapter = CardAdapter(
                words,
                onItemClick = { word ->
                    // Сохраняем позицию прокрутки перед переходом к редактированию
                    recyclerViewState = binding.wordRecyclerView.layoutManager?.onSaveInstanceState()
                    val editFragment = AddWordFragment.newInstance(word)
                    replaceFragment(editFragment)
                },
                collectionIdToName = collectionIdToName
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
        selectedCollectionPosition = binding.collectionSpinner.selectedItemPosition
        recyclerViewState = binding.wordRecyclerView.layoutManager?.onSaveInstanceState()
    }
}