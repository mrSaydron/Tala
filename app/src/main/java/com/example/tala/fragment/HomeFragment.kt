package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tala.R
import com.example.tala.databinding.FragmentHomeBinding
import com.example.tala.entity.category.Category
import com.example.tala.entity.category.CategoryViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tala.fragment.adapter.CategoryAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private lateinit var categoryAdapter: CategoryAdapter
    private val categories = mutableListOf<Category>()

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
        categoryAdapter = CategoryAdapter(
            categories,
            onCategoryClick = { category ->
                val reviewFragment = ReviewFragment.newInstance(category.id)
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
        val viewModel = ViewModelProvider(requireActivity())[CategoryViewModel::class.java]
        viewModel.getAllCategories().observe(viewLifecycleOwner) { categoryList ->
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