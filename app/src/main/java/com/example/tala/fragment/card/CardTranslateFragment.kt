package com.example.tala.fragment.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.tala.MainActivity
import com.example.tala.databinding.FragmentCardTranslateBinding
import com.example.tala.model.dto.info.WordCardInfo

class CardTranslateFragment(private val getInfo: () -> WordCardInfo) : CardReviewBase() {

    private lateinit var binding: FragmentCardTranslateBinding
    private var info: WordCardInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCardTranslateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        info = getInfo()
        bind()

        info?.english?.let { MainActivity.textToSpeechHelper.speak(it) }

        binding.playButton.setOnClickListener {
            info?.english?.let { MainActivity.textToSpeechHelper.speak(it) }
        }
    }

    override fun roll() {
        binding.answerTextView.visibility = View.VISIBLE
        binding.wordImageView.visibility = View.VISIBLE
    }

    override fun bind() {
        info?.let { data ->
            binding.wordTextView.text = data.english
            binding.answerTextView.text = data.russian
            if (!data.hint.isNullOrEmpty()) {
                binding.hintTextView.text = data.hint
                binding.hintTextView.visibility = View.VISIBLE
            } else {
                binding.hintTextView.visibility = View.GONE
            }
            data.imagePath?.let { path ->
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