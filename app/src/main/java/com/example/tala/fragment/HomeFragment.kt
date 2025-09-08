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

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private lateinit var categoryAdapter: CollectionAdapter
    private val categories = mutableListOf<CardCollection>()

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
        binding.categoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        categoryAdapter = CollectionAdapter(
            categories,
            onCollectionClick = { collection ->
                val reviewFragment = ReviewFragment.newInstance(collection.id)
                replaceFragment(reviewFragment)
            },
            cardViewModelProvider = { ViewModelProvider(this)[com.example.tala.entity.card.CardViewModel::class.java] },
            lifecycleOwner = viewLifecycleOwner
        )
        binding.categoryRecyclerView.adapter = categoryAdapter

        // Загрузка категорий
        loadCategories()

        // Переход к обучению по нажатию выполняется через адаптер

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
        val viewModel = ViewModelProvider(requireActivity())[CollectionViewModel::class.java]
        viewModel.getAllCollections().observe(viewLifecycleOwner) { categoryList ->
            categories.clear()
            categories.addAll(categoryList)
            categoryAdapter.updateData(categories)
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