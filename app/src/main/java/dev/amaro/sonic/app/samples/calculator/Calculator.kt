package dev.amaro.sonic.app.samples.calculator

import dev.amaro.sonic.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

object Calculator {

    class OperationParser : IMiddleware<State> {
        override fun process(action: IAction, state: State, processor: IProcessor<State>) {
            if (action is Action.OperationChoice) {
                listOf(
                    Operation.Add,
                    Operation.Division,
                    Operation.Multiply,
                    Operation.Subtract
                ).firstOrNull { it.symbol == action.symbol }?.run {
                    processor.reduce(Action.OperationCommand(this))
                }
            }
        }
    }

    class Calculation : IMiddleware<State> {
        override fun process(action: IAction, state: State, processor: IProcessor<State>) {
            if (action is Action.SecondNumber || action is Action.FirstNumber || action is Action.Restart) {
                processor.reduce(action)
            }
            if (state.firstNumber != null && (state.secondNumber != null || action is Action.SecondNumber) && state.operation != null) {
                val secondNumber = (state.secondNumber ?: (action as Action.SecondNumber).number)
                val result = when (state.operation) {
                    Operation.Add -> state.firstNumber + secondNumber
                    Operation.Multiply -> state.firstNumber * secondNumber
                    Operation.Subtract -> state.firstNumber - secondNumber
                    Operation.Division -> state.firstNumber / secondNumber
                }
                processor.reduce(Action.SetResult(result))
            }
        }
    }

    class SimpleStateManager :
        StateManager<State>(State(), listOf(OperationParser(), Calculation())) {
        override val reducer: IReducer<State> = object : IReducer<State> {
            override fun reduce(action: IAction, currentState: State): State {
                return when (action) {
                    is Action.FirstNumber -> currentState.copy(firstNumber = action.number)
                    is Action.SecondNumber -> currentState.copy(secondNumber = action.number)
                    is Action.SetResult -> currentState.copy(result = action.number)
                    is Action.OperationCommand -> currentState.copy(operation = action.operation)
                    is Action.Restart -> State()
                    else -> State()
                }
            }

        }
    }

    class SimpleScreen(
        renderer: IRenderer<State>,
        collectScope: CoroutineDispatcher = Dispatchers.Default
    ) :
        Screen<State>(SimpleStateManager(), renderer, collectScope)

    sealed class Action : IAction {
        object Restart : Action()
        data class FirstNumber(val number: Int) : Action()
        data class SecondNumber(val number: Int) : Action()
        data class SetResult(val number: Int) : Action()
        data class OperationChoice(val symbol: String) : Action()
        data class OperationCommand(val operation: Operation) : Action()
    }

    sealed class Operation(val symbol: String) {
        object Add : Operation("+")
        object Subtract : Operation("-")
        object Multiply : Operation("*")
        object Division : Operation("/")
    }

    data class State(
        val firstNumber: Int? = null,
        val secondNumber: Int? = null,
        val operation: Operation? = null,
        val result: Int? = null
    )
}

