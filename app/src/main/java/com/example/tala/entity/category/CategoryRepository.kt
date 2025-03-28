package com.example.tala.entity.category

import androidx.lifecycle.LiveData

class CategoryRepository(private val categoryDao: CategoryDao) {

    suspend fun insert(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun delete(category: Category) {
        categoryDao.delete(category)
    }

    fun getAllCategories(): LiveData<List<Category>> {
        return categoryDao.getAllCategories()
    }

    suspend fun deleteAllCategories() {
        categoryDao.deleteAllCategories()
    }
}