package com.example.tala.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.tala.databinding.FragmentTranslateCardTypeBinding
import com.example.tala.model.dto.lessonCard.TranslateLessonCardDto

class TranslateCardTypeFragment : Fragment() {

    private var _binding: FragmentTranslateCardTypeBinding? = null
    private val binding get() = _binding!!

    private val dto: TranslateLessonCardDto by lazy {
        @Suppress("DEPRECATION")
        return@lazy requireArguments().getParcelable<TranslateLessonCardDto>(ARG_CARD_DTO)
            ?: throw IllegalStateException("TranslateLessonCardDto is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslateCardTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.translateCardWordTextView.text = dto.word
        binding.translateCardTranslationTextView.text = dto.translation

        val info = dto.info.orEmpty()
        if (info.isBlank()) {
            binding.translateCardInfoTextView.visibility = View.GONE
        } else {
            binding.translateCardInfoTextView.visibility = View.VISIBLE
            binding.translateCardInfoTextView.text = info
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CARD_DTO = "translate_card_dto"

        fun newInstance(dto: TranslateLessonCardDto): TranslateCardTypeFragment {
            val fragment = TranslateCardTypeFragment()
            fragment.arguments = Bundle().apply {
                putParcelable(ARG_CARD_DTO, dto)
            }
            return fragment
        }
    }
}

