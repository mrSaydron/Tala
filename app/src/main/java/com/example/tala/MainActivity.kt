package com.example.tala

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.tala.TalaDatabase
import com.example.tala.entity.dictionary.DictionaryRepository
import com.example.tala.entity.dictionaryCollection.DictionaryCollectionRepository
import com.example.tala.entity.lesson.LessonRepository
import com.example.tala.entity.lessoncardtype.LessonCardTypeRepository
import com.example.tala.entity.lessonprogress.LessonProgressRepository
import com.example.tala.fragment.HomeFragment
import com.example.tala.model.enums.CardTypeEnum
import com.example.tala.service.TextToSpeechHelper
import com.example.tala.service.lessonCard.LessonCardService
import com.example.tala.service.lessonCard.LessonCardTypeService
import com.example.tala.service.lessonCard.TranslateLessonCardTypeService
import com.example.tala.service.lessonCard.ReverseTranslateLessonCardTypeService
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        APP = this
        lessonCardService = buildLessonCardService()

        enableEdgeToEdge() // Для красивого отображения на edge-to-edge экранах
        setContentView(R.layout.activity_main)

        // Добавляем системные отступы снизу и по бокам; сверху оставляем 0, чтобы контент мог заходить под статус-бар
        val container = findViewById<android.view.View>(R.id.fragmentContainer)
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = max(systemBars.bottom, imeInsets.bottom)
            view.setPadding(systemBars.left, 0, systemBars.right, bottomInset)
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
        lateinit var lessonCardService: LessonCardService
    }

    private fun buildLessonCardService(): LessonCardService {
        val database = TalaDatabase.getDatabase(this)
        val lessonRepository = LessonRepository(database.lessonDao())
        val lessonCardTypeRepository = LessonCardTypeRepository(database.lessonCardTypeDao())
        val dictionaryRepository = DictionaryRepository(database.dictionaryDao())
        val dictionaryCollectionRepository = DictionaryCollectionRepository(
            database.dictionaryCollectionDao(),
            database.dictionaryCollectionEntryDao()
        )
        val lessonProgressRepository = LessonProgressRepository(database.lessonProgressDao())

        val translateService = TranslateLessonCardTypeService(
            lessonProgressRepository,
            dictionaryRepository
        )
        val reverseTranslateService = ReverseTranslateLessonCardTypeService(
            lessonProgressRepository,
            dictionaryRepository
        )

        val services = mapOf<CardTypeEnum, LessonCardTypeService>(
            CardTypeEnum.TRANSLATE to translateService,
            CardTypeEnum.REVERSE_TRANSLATE to reverseTranslateService
        )

        return LessonCardService(
            lessonRepository = lessonRepository,
            lessonCardTypeRepository = lessonCardTypeRepository,
            dictionaryCollectionRepository = dictionaryCollectionRepository,
            dictionaryRepository = dictionaryRepository,
            lessonProgressRepository = lessonProgressRepository,
            typeServices = services
        )
    }
}
