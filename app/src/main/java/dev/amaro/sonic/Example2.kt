package dev.amaro.sonic

object Example2 {

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
            when (action) {
                is Action.FirstNumber, is Action.Restart -> {
                    processor.reduce(action)
                }
                is Action.SecondNumber -> {
                    val result = when (state.operation) {
                        Operation.Add -> state.firstNumber!! + action.number
                        Operation.Multiply -> state.firstNumber!! * action.number
                        Operation.Subtract -> state.firstNumber!! - action.number
                        Operation.Division -> state.firstNumber!! / action.number
                        else -> 0
                    }
                    processor.reduce(action)
                    processor.reduce(Action.SetResult(result))
                }
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

    class SimpleScreen : Screen<State>(SimpleStateManager()) {

        init {
            perform(Action.Restart)
        }

        override fun render(state: State) {
            when {
                state.firstNumber == null -> {
                    println("Enter the first number: ")
                    perform(Action.FirstNumber(readLine()?.toInt() ?: 0))
                }
                state.operation == null -> {
                    println("Enter the operation: ")
                    perform(Action.OperationChoice(readLine() ?: ""))
                }
                state.secondNumber == null -> {
                    println("Enter the second number: ")
                    perform(Action.SecondNumber(readLine()?.toInt() ?: 0))
                }
                else -> {
                    println("The result is: ${state.firstNumber} ${state.operation.symbol} ${state.secondNumber} = ${state.result}")
                    println("Press enter to restart")
                    readLine()
                    perform(Action.Restart)
                }
            }
        }
    }

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

