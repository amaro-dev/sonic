package dev.amaro.sonic

object Example1 {
    class SimpleStateManager : StateManager<State>(State()) {
        override val processor: IProcessor<State> = object : Processor<State>(this) {
            override fun reduce(action: IAction) {
                when (action) {
                    is Action.InputNumber -> {
                        state.value =
                            state.value.copy(number = action.number, result = action.number * 2)
                    }
                    is Action.Restart -> {
                        state.value = State()
                    }
                }
            }
        }
    }

    class SimpleScreen : Screen<State>(SimpleStateManager()) {

        init {
            perform(Action.Restart)
        }

        override fun render(state: State) {
            if (state.number == null) {
                println("Enter a number: ")
                perform(Action.InputNumber(readLine()?.toInt() ?: 0))
            } else {
                println("The number you've entered is ${state.number} and the result is ${state.result}")
                println("Press enter to restart")
                readLine()
                perform(Action.Restart)
            }
        }
    }

    sealed class Action : IAction {
        data class InputNumber(val number: Int) : Action()
        object Restart : Action()

        // data class FirstNumber(val number: Int) : Action()
        // data class SecondNumber(val number: Int) : Action()
        // data class Operation(val symbol: String) : Action()
    }

// sealed class Operation {
//     object Add : Operation()
//     object Subtract : Operation()
//     object Multiply : Operation()
//     object Division : Operation()
// }

    data class State(
        val number: Int? = null,
        val result: Int? = null
    ) {
        // fun isEmpty(): Boolean {
        //     return firstNumber?.let { secondNumber?.let { operation?.let { true } } } ?: false
        // }
    }
}

