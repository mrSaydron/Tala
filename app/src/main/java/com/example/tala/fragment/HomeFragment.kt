package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tala.R
import com.example.tala.databinding.FragmentHomeBinding
import com.example.tala.entity.collection.CardCollection
import com.example.tala.entity.collection.CollectionViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tala.fragment.adapter.CollectionAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tala.entity.card.CardViewModel

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private lateinit var collectionAdapter: CollectionAdapter
    private val categories = mutableListOf<CardCollection>()
    private lateinit var cardViewModel: CardViewModel
    private var lastCollections: List<CardCollection> = emptyList()
    private var defaultHasCards: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Корректная отрисовка под статус/навигационные панели
        val initialPaddingLeft = binding.root.paddingLeft
        val initialPaddingTop = binding.root.paddingTop
        val initialPaddingRight = binding.root.paddingRight
        val initialPaddingBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingLeft,
                initialPaddingTop + systemBars.top,
                initialPaddingRight,
                initialPaddingBottom + systemBars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }

        // Инициализация RecyclerView категорий
        binding.collectionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        collectionAdapter = CollectionAdapter(
            categories,
            onCollectionClick = { collection ->
                val reviewFragment = ReviewFragment.newInstance(collection.id)
                replaceFragment(reviewFragment)
            },
            cardViewModelProvider = { ViewModelProvider(this)[com.example.tala.entity.card.CardViewModel::class.java] },
            lifecycleOwner = viewLifecycleOwner
        )
        binding.collectionRecyclerView.adapter = collectionAdapter

        // ViewModel карточек
        cardViewModel = ViewModelProvider(requireActivity())[CardViewModel::class.java]

        // Загрузка категорий
        loadCategories()

        // Переход к обучению по нажатию выполняется через адаптер

        binding.wordListButton.setOnClickListener {
            val wordListFragment = WordListFragment()
            replaceFragment(wordListFragment)
        }

        binding.dictionaryButton.setOnClickListener {
            val dictionaryFragment = DictionaryListFragment.newInstance()
            replaceFragment(dictionaryFragment)
        }

        binding.collectionListButton.setOnClickListener {
            val collectionListFragment = CollectionListFragment()
            replaceFragment(collectionListFragment)
        }

        binding.lessonListButton.setOnClickListener {
            val lessonListFragment = LessonListFragment()
            replaceFragment(lessonListFragment)
        }

        binding.settingsButton.setOnClickListener {
            val settingsFragment = SettingsFragment()
            replaceFragment(settingsFragment)
        }
    }

    // Загрузка категорий
    private fun loadCategories() {
        val viewModel = ViewModelProvider(requireActivity())[CollectionViewModel::class.java]
        viewModel.getAllCollections().observe(viewLifecycleOwner) { collectionList ->
            lastCollections = collectionList
            // Настраиваем наблюдение за Default один раз, когда она появится
            val defaultCollection = collectionList.firstOrNull { it.name == "Default" }
            if (defaultCollection != null) {
                cardViewModel.getCardCountByCollection(defaultCollection.id).observe(viewLifecycleOwner) { count ->
                    defaultHasCards = (count ?: 0) > 0
                    refreshDisplayedCategories()
                }
            }
            refreshDisplayedCategories()
        }
    }

    private fun refreshDisplayedCategories() {
        val filtered = lastCollections.filter { it.name != "Default" || defaultHasCards }
        categories.clear()
        categories.addAll(filtered)
        collectionAdapter.updateData(categories)
    }

    // Замена фрагмента
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}