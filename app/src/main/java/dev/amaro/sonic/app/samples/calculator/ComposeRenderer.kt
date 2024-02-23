package dev.amaro.sonic.app.samples.calculator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.amaro.sonic.IAction

@Composable
fun ComposeRenderer(stateManager: Calculator.SimpleStateManager) {
    val state = stateManager.listen().collectAsState().value

    Scaffold(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(it)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = state.firstNumber?.toString() ?: "",
                    onValueChange = {
                        stateManager.perform(Calculator.Action.FirstNumber(it.toInt()))
                    })
                Spacer(modifier = Modifier.width(16.dp))
                TextField(
                    modifier = Modifier.weight(1f),
                    value = state.secondNumber?.toString() ?: "",
                    onValueChange = {
                        stateManager.perform(Calculator.Action.SecondNumber(it.toInt()))
                    })
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MyRadioButton("+", "Sum", state.operation) { stateManager.perform(it) }
                Spacer(modifier = Modifier.width(16.dp))
                MyRadioButton("-", "Subtract", state.operation) { stateManager.perform(it) }
                Spacer(modifier = Modifier.width(16.dp))
                MyRadioButton("/", "Divide", state.operation) { stateManager.perform(it) }
                Spacer(modifier = Modifier.width(16.dp))
                MyRadioButton("*", "Multiply", state.operation) { stateManager.perform(it) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Result is: ${state.result ?: ""}",
                modifier = Modifier.align(CenterHorizontally)
            )
        }
    }


}

@Composable
fun RowScope.MyRadioButton(
    symbol: String,
    text: String,
    current: Calculator.Operation?,
    onSelect: (IAction) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)) {
        RadioButton(
            selected = current?.symbol == symbol,
            onClick = { onSelect(Calculator.Action.OperationChoice(symbol)) }
        )
        Text(text)
    }
}