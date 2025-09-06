package com.example.tala.fragment.card

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.tala.MainActivity
import com.example.tala.databinding.FragmentCardReverseTranslateBinding
import com.example.tala.model.dto.info.WordCardInfo

class CardReverseTranslateFragment : CardReviewBase() {

    private lateinit var binding: FragmentCardReverseTranslateBinding
    private var info: WordCardInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        info = arguments?.getSerializable(ARG_INFO) as? WordCardInfo
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCardReverseTranslateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bind()

        binding.playButton.setOnClickListener {
            try {
                Log.i("CardReverseTranslateFragment", "onViewCreated: ${info?.english}")
                info?.english?.let { MainActivity.textToSpeechHelper.speak(it) }
            } catch (e: Exception) {
                Log.i("CardReverseTranslateFragment", "onViewCreated: $e")
            }
        }
    }

    override fun roll() {
        binding.wordTextView.visibility = View.VISIBLE

        info?.english?.let { MainActivity.textToSpeechHelper.speak(it) }
        binding.playButton.visibility = View.VISIBLE
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
            binding.wordTextView.visibility = View.GONE
            binding.playButton.visibility = View.GONE
        }
    }

    companion object {
        private const val ARG_INFO = "info"

        fun newInstance(info: WordCardInfo): CardReverseTranslateFragment {
            val fragment = CardReverseTranslateFragment()
            val args = Bundle()
            args.putSerializable(ARG_INFO, info)
            fragment.arguments = args
            return fragment
        }
    }
}