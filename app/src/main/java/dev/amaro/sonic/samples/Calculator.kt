package dev.amaro.sonic.samples

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
            if (state.firstNumber != null && state.secondNumber != null && state.operation != null) {
                val result = when (state.operation) {
                    Operation.Add -> state.firstNumber + state.secondNumber
                    Operation.Multiply -> state.firstNumber * state.secondNumber
                    Operation.Subtract -> state.firstNumber - state.secondNumber
                    Operation.Division -> state.firstNumber / state.secondNumber
                }
                processor.reduce(Action.SetResult(result))
            }
        }
    }

    class SimpleStateManager :
        StateManager<State>(State(), listOf(OperationParser(), Calculation())) {
        override val processor: IProcessor<State> = object : Processor<State>(this) {
            override fun reduce(action: IAction) {
                state.value = when (action) {
                    is Action.FirstNumber -> state.value.copy(firstNumber = action.number)
                    is Action.SecondNumber -> state.value.copy(secondNumber = action.number)
                    is Action.SetResult -> state.value.copy(result = action.number)
                    is Action.OperationCommand -> state.value.copy(operation = action.operation)
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

