package dev.amaro.sonic.app.samples.converter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import dev.amaro.sonic.IPerformer
import dev.amaro.sonic.IRenderer
import dev.amaro.sonic.app.R
import dev.amaro.sonic.app.clicks
import dev.amaro.sonic.app.selection
import dev.amaro.sonic.app.toFlow
import dev.amaro.sonic.collectOn
import kotlinx.coroutines.Dispatchers

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), IRenderer<Converter.State> {

    private lateinit var fieldAmount: EditText
    private lateinit var spinnerSource: Spinner
    private lateinit var spinnerTarget: Spinner
    private lateinit var textResult: TextView
    private lateinit var button: Button


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Converter.Screen(this)
        fieldAmount = view.findViewById(R.id.fieldAmount)
        spinnerSource = view.findViewById(R.id.spinnerSource)
        spinnerTarget = view.findViewById(R.id.spinnerTarget)
        textResult = view.findViewById(R.id.textResult)
        button = view.findViewById(R.id.button)

    }

    override fun render(state: Converter.State, performer: IPerformer<Converter.State>) {
        spinnerSource.onItemSelectedListener = null
        spinnerTarget.onItemSelectedListener = null
        val sourceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            state.sourceOptions.map { it?.symbol ?: "" })
        spinnerSource.adapter = sourceAdapter
        spinnerSource.setSelection(sourceAdapter.getPosition(state.source))

        val targetAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            state.targetOptions.map { it?.symbol ?: "" })
        spinnerTarget.adapter = targetAdapter
        spinnerTarget.setSelection(targetAdapter.getPosition(state.target))

        spinnerTarget.selection<String>()
            .collectOn(Dispatchers.Main) { performer.perform(Converter.Action.SetTarget(it)) }
        spinnerSource.selection<String>()
            .collectOn(Dispatchers.Main) { performer.perform(Converter.Action.SetSource(it)) }
        fieldAmount.toFlow()
            .collectOn(Dispatchers.Main) { performer.perform(Converter.Action.SetAmount(it.toString())) }
        button.clicks()
            .collectOn(Dispatchers.Main) { performer.perform(Converter.Action.SwitchCurrencies) }
        textResult.text = state.result?.toString() ?: ""
    }
}