package com.example.tala.fragment.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Space
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.tala.R
import com.example.tala.fragment.model.CardTypeConditionArgs
import com.example.tala.model.enums.CardTypeEnum
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CollectionCardTypeSelectionAdapter(
    private val availableCardTypes: List<CardTypeEnum>,
    private val activeTypesProvider: () -> List<CardTypeEnum>,
    private val onToggle: (CardTypeEnum, Boolean) -> Unit,
    private val onUpdate: (CardTypeConditionArgs) -> Unit
) : RecyclerView.Adapter<CollectionCardTypeSelectionAdapter.ViewHolder>() {

    private val items: MutableList<CardTypeConditionArgs> = mutableListOf()

    fun submit(items: List<CardTypeConditionArgs>) {
        this.items.clear()
        this.items.addAll(items.sortedBy { it.cardType.ordinal })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_card_type_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.cardTypeOptionTitleTextView)
        private val toggleSwitch: SwitchMaterial = itemView.findViewById(R.id.cardTypeOptionSwitch)
        private val onDropdownLayout: TextInputLayout = itemView.findViewById(R.id.cardTypeOnDropdownLayout)
        private val onDropdown: AutoCompleteTextView = itemView.findViewById(R.id.cardTypeOnDropdown)
        private val onValueLayout: TextInputLayout = itemView.findViewById(R.id.cardTypeOnValueLayout)
        private val onValueInput: TextInputEditText = itemView.findViewById(R.id.cardTypeOnValueInput)
        private val offDropdownLayout: TextInputLayout = itemView.findViewById(R.id.cardTypeOffDropdownLayout)
        private val offDropdown: AutoCompleteTextView = itemView.findViewById(R.id.cardTypeOffDropdown)
        private val offValueLayout: TextInputLayout = itemView.findViewById(R.id.cardTypeOffValueLayout)
        private val offValueInput: TextInputEditText = itemView.findViewById(R.id.cardTypeOffValueInput)

        private var isBinding = false

        fun bind(item: CardTypeConditionArgs) {
            isBinding = true
            titleTextView.text = item.cardType.titleRu

            val normalizedItem = normalizeSelection(item)
            setupDropdowns(normalizedItem)
            setupValues(normalizedItem)
            applyEnabledState(normalizedItem)

            toggleSwitch.setOnCheckedChangeListener(null)
            toggleSwitch.isChecked = normalizedItem.enabled
            toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
                onToggle(normalizedItem.cardType, isChecked)
                applyEnabledState(normalizedItem.copy(enabled = isChecked))
            }
            isBinding = false
        }

        private fun setupDropdowns(item: CardTypeConditionArgs) {
            val context = itemView.context
            val noneLabel = context.getString(R.string.collection_card_type_condition_none)

            val activeTypes = activeTypesProvider()
            val onOptions = listOf<CardTypeEnum?>(null) + activeTypes.filter { it != item.cardType }
            val offOptions = listOf<CardTypeEnum?>(null) + activeTypes

            val onAdapter = ArrayAdapter(
                context,
                android.R.layout.simple_list_item_1,
                onOptions.map { it?.titleRu ?: noneLabel }
            )
            val offAdapter = ArrayAdapter(
                context,
                android.R.layout.simple_list_item_1,
                offOptions.map { it?.titleRu ?: noneLabel }
            )

            onDropdown.setAdapter(onAdapter)
            offDropdown.setAdapter(offAdapter)

            onDropdown.setOnItemClickListener { _, _, position, _ ->
                if (isBinding) return@setOnItemClickListener
                val selected = onOptions[position]
                val nextValue = item.conditionOnValue ?: DEFAULT_THRESHOLD
                if (selected == null) {
                    onValueInput.setText("")
                    onUpdate(item.copy(conditionOnType = null, conditionOnValue = null))
                } else {
                    if (onValueInput.text.isNullOrBlank()) {
                        onValueInput.setText(DEFAULT_THRESHOLD.toString())
                    }
                    onUpdate(item.copy(conditionOnType = selected, conditionOnValue = nextValue))
                }
                applyEnabledState(currentUiState(item.copy(conditionOnType = selected)))
            }

            offDropdown.setOnItemClickListener { _, _, position, _ ->
                if (isBinding) return@setOnItemClickListener
                val selected = offOptions[position]
                val nextValue = item.conditionOffValue ?: DEFAULT_THRESHOLD
                if (selected == null) {
                    offValueInput.setText("")
                    onUpdate(item.copy(conditionOffType = null, conditionOffValue = null))
                } else {
                    if (offValueInput.text.isNullOrBlank()) {
                        offValueInput.setText(DEFAULT_THRESHOLD.toString())
                    }
                    onUpdate(item.copy(conditionOffType = selected, conditionOffValue = nextValue))
                }
                applyEnabledState(currentUiState(item.copy(conditionOffType = selected)))
            }

            val onText = item.conditionOnType?.titleRu ?: noneLabel
            val offText = item.conditionOffType?.titleRu ?: noneLabel
            onDropdown.setText(onText, false)
            offDropdown.setText(offText, false)
        }

        private fun setupValues(item: CardTypeConditionArgs) {
            onValueInput.doAfterTextChanged { text ->
                if (isBinding) return@doAfterTextChanged
                val value = text?.toString()?.toIntOrNull()
                onUpdate(item.copy(conditionOnValue = value))
            }
            offValueInput.doAfterTextChanged { text ->
                if (isBinding) return@doAfterTextChanged
                val value = text?.toString()?.toIntOrNull()
                onUpdate(item.copy(conditionOffValue = value))
            }

            isBinding = true
            onValueInput.setText(item.conditionOnValue?.toString().orEmpty())
            offValueInput.setText(item.conditionOffValue?.toString().orEmpty())
            isBinding = false
        }

        private fun applyEnabledState(item: CardTypeConditionArgs) {
            val onEnabled = item.enabled && item.conditionOnType != null
            val offEnabled = item.enabled && item.conditionOffType != null

            setEnabled(onDropdownLayout, item.enabled)
            setEnabled(onDropdown, item.enabled)
            setEnabled(onValueLayout, onEnabled)
            setEnabled(onValueInput, onEnabled)

            setEnabled(offDropdownLayout, item.enabled)
            setEnabled(offDropdown, item.enabled)
            setEnabled(offValueLayout, offEnabled)
            setEnabled(offValueInput, offEnabled)
        }

        private fun setEnabled(view: View, enabled: Boolean) {
            view.isEnabled = enabled
            if (view is TextInputLayout) {
                view.isHintEnabled = enabled
            }
        }

        private fun currentUiState(item: CardTypeConditionArgs): CardTypeConditionArgs {
            return item.copy(
                enabled = toggleSwitch.isChecked
            )
        }

        private fun normalizeSelection(item: CardTypeConditionArgs): CardTypeConditionArgs {
            val activeTypes = activeTypesProvider()
            var normalized = item
            if (normalized.conditionOnType != null && normalized.conditionOnType !in activeTypes) {
                normalized = normalized.copy(conditionOnType = null, conditionOnValue = null)
                onUpdate(normalized)
            }
            if (normalized.conditionOffType != null && normalized.conditionOffType !in activeTypes) {
                normalized = normalized.copy(conditionOffType = null, conditionOffValue = null)
                onUpdate(normalized)
            }
            return normalized
        }
    }

    companion object {
        private const val DEFAULT_THRESHOLD = 10
    }
}
