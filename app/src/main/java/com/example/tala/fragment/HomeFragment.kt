package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация списка категорий
        categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.categoryListView.adapter = categoryAdapter

        // Загрузка категорий
        loadCategories()

        // Переход к обучению по нажатию на категорию
        binding.categoryListView.setOnItemClickListener { _, _, position, _ ->
            val categoryId = categories[position].id
            val reviewFragment = ReviewFragment.newInstance(categoryId)
            replaceFragment(reviewFragment)
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