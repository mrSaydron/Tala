package com.example.tala

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max
import com.example.tala.fragment.HomeFragment
import com.example.tala.service.TextToSpeechHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        APP = this

        enableEdgeToEdge() // Для красивого отображения на edge-to-edge экранах
        setContentView(R.layout.activity_main)

        // Добавляем системные отступы, чтобы контент не уходил под статус/навигационную панель
        val container = findViewById<android.view.View>(R.id.fragmentContainer)
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = max(systemBars.bottom, imeInsets.bottom)
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomInset)
            insets
        }

        textToSpeechHelper = TextToSpeechHelper(this) { isInitialized ->
            if (!isInitialized) {
                Toast.makeText(this, "Озвучка не поддерживается на этом устройстве", Toast.LENGTH_SHORT).show()
            }
        }

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
        lateinit var textToSpeechHelper: TextToSpeechHelper
    }
}
