package com.example.tala.fragment.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.tala.MainActivity
import com.example.tala.databinding.FragmentCardTranslateBinding
import com.example.tala.entity.card.Card

class CardTranslateFragment(private val getCard: () -> Card) : CardReviewBase() {

    private lateinit var binding: FragmentCardTranslateBinding
    private var card: Card? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCardTranslateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        card = getCard()
        bind()

        card?.let { MainActivity.textToSpeechHelper.speak(card!!.english) }

        binding.playButton.setOnClickListener {
            card?.let { MainActivity.textToSpeechHelper.speak(it.english) }
        }
    }

    override fun roll() {
        binding.answerTextView.visibility = View.VISIBLE
        binding.wordImageView.visibility = View.VISIBLE
    }

    override fun bind() {
        card?.let {
            binding.wordTextView.text = it.english
            binding.answerTextView.text = it.russian
            // показать подсказку, если есть
            val hint = try {
                it.info?.let { info -> org.json.JSONObject(info).optString("hint", "") }
            } catch (_: Exception) { "" }
            if (!hint.isNullOrEmpty()) {
                binding.hintTextView.text = hint
                binding.hintTextView.visibility = View.VISIBLE
            } else {
                binding.hintTextView.visibility = View.GONE
            }
            it.imagePath?.let { path ->
                Glide.with(this)
                    .load(path)
                    .into(binding.wordImageView)
            }
            binding.answerTextView.visibility = View.GONE
            binding.wordImageView.visibility = View.GONE
            binding.playButton.visibility = View.VISIBLE
        }
    }

}