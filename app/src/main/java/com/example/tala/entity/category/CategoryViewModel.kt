package com.example.tala.entity.category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.tala.TalaDatabase
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CategoryRepository

    init {
        val categoryDao = TalaDatabase.getDatabase(application).categoryDao()
        repository = CategoryRepository(categoryDao)
    }

    fun insertCategory(category: Category) = viewModelScope.launch {
        repository.insert(category)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        repository.delete(category)
    }

    fun getAllCategories(): LiveData<List<Category>> {
        return repository.getAllCategories()
    }

    suspend fun deleteAllCategories() {
        repository.deleteAllCategories()
    }
}