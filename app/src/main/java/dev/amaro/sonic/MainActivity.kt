package dev.amaro.sonic

import android.os.Bundle
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Example2.SimpleScreen(ActivityRenderer(this), Dispatchers.Main)
    }
}

class ActivityRenderer(activity: AppCompatActivity) : IRenderer<Example2.State> {
    private val number1: EditText = activity.findViewById(R.id.number1)
    private val number2: EditText = activity.findViewById(R.id.number2)
    private val operation: RadioGroup = activity.findViewById(R.id.radioGroup)
    private val result: TextView = activity.findViewById(R.id.result)
    override fun render(state: Example2.State, performer: IPerformer<Example2.State>) {
        number1.doOnTextChanged { text, _, _, _ ->
            text.takeIf { it.toString().isDigitsOnly() && it!!.isNotEmpty() }?.run {
                performer.perform(Example2.Action.FirstNumber(this.toString().toInt()))
            }
        }
        number2.doOnTextChanged { text, _, _, _ ->
            text.takeIf { it.toString().isDigitsOnly() && it!!.isNotEmpty() }?.run {
                performer.perform(Example2.Action.SecondNumber(this.toString().toInt()))
            }
        }
        operation.setOnCheckedChangeListener { i, _ ->
            val symbol = i.findViewById<RadioButton>(i.checkedRadioButtonId).tag as String
            performer.perform(Example2.Action.OperationChoice(symbol))
        }
        if (state.result != null) {
            result.text = state.result.toString()
        }
    }
}