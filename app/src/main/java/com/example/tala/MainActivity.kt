package com.example.tala

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.tala.entity.category.CategoryViewModel
import com.example.tala.entity.learningMode.LearningMode
import com.example.tala.entity.learningMode.LearningModeViewModel
import com.example.tala.fragment.HomeFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        APP = this

        enableEdgeToEdge() // Для красивого отображения на edge-to-edge экранах
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }
    }

    // Функция для замены фрагмента
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    companion object {
        lateinit var APP: AppCompatActivity
    }
}
