package dev.amaro.sonic.samples

import dev.amaro.sonic.IPerformer
import dev.amaro.sonic.IRenderer

class TerminalRenderer : IRenderer<Calculator.State> {
    override fun render(state: Calculator.State, performer: IPerformer<Calculator.State>) {
        when {
            state.firstNumber == null -> {
                println("Enter the first number: ")
                performer.perform(Calculator.Action.FirstNumber(readLine()?.toInt() ?: 0))
            }
            state.operation == null -> {
                println("Enter the operation: ")
                performer.perform(Calculator.Action.OperationChoice(readLine() ?: ""))
            }
            state.secondNumber == null -> {
                println("Enter the second number: ")
                performer.perform(Calculator.Action.SecondNumber(readLine()?.toInt() ?: 0))
            }
            else -> {
                println("The result is: ${state.firstNumber} ${state.operation.symbol} ${state.secondNumber} = ${state.result}")
                println("Press enter to restart")
                readLine()
                performer.perform(Calculator.Action.Restart)
            }
        }
    }
}