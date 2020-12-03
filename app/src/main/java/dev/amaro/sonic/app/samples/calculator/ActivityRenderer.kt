package dev.amaro.sonic.app.samples.calculator

import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.widget.doOnTextChanged
import dev.amaro.sonic.IPerformer
import dev.amaro.sonic.IRenderer
import dev.amaro.sonic.app.R

class ActivityRenderer(activity: AppCompatActivity) : IRenderer<Calculator.State> {
    private val number1: EditText = activity.findViewById(R.id.number1)
    private val number2: EditText = activity.findViewById(R.id.number2)
    private val operation: RadioGroup = activity.findViewById(R.id.radioGroup)
    private val result: TextView = activity.findViewById(R.id.result)
    override fun render(state: Calculator.State, performer: IPerformer<Calculator.State>) {
        number1.doOnTextChanged { text, _, _, _ ->
            text.takeIf { it.toString().isDigitsOnly() && it!!.isNotEmpty() }?.run {
                performer.perform(Calculator.Action.FirstNumber(this.toString().toInt()))
            }
        }
        number2.doOnTextChanged { text, _, _, _ ->
            text.takeIf { it.toString().isDigitsOnly() && it!!.isNotEmpty() }?.run {
                performer.perform(Calculator.Action.SecondNumber(this.toString().toInt()))
            }
        }
        operation.setOnCheckedChangeListener { i, _ ->
            val symbol = i.findViewById<RadioButton>(i.checkedRadioButtonId).tag as String
            performer.perform(Calculator.Action.OperationChoice(symbol))
        }
        if (state.result != null) {
            result.text = state.result.toString()
        }
    }
}