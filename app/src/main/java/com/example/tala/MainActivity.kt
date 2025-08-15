package com.example.tala

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.tala.fragment.HomeFragment
import com.example.tala.service.TextToSpeechHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        APP = this

        enableEdgeToEdge() // Для красивого отображения на edge-to-edge экранах
        setContentView(R.layout.activity_main)

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
