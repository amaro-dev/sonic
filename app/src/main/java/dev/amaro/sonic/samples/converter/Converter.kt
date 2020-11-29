package dev.amaro.sonic.samples.converter

import dev.amaro.sonic.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.math.BigDecimal

object Converter {

    class Screen(
        renderer: IRenderer<State>,
        initialState : State = State(),
        dispatcher: CoroutineDispatcher = Dispatchers.Main
    ) :
        dev.amaro.sonic.Screen<State>(SimpleStateManager(initialState), renderer, dispatcher)

    class SimpleStateManager(initialState: State) :
        StateManager<State>(
            initialState,
            listOf(
                AmountValidator(),
                CurrencySelection()
            )
        ) {
        override val processor: IProcessor<State> = object : Processor<State>(this) {
            override fun reduce(action: IAction) {
                state.value = when (action) {
                    is Action.SetSource -> state.value.copy(source = action.currency)
                    is Action.SetTarget -> state.value.copy(target = action.currency)
                    is Action.SetAmount -> state.value.copy(amount = action.amount.toBigDecimal())
                    is Action.SwitchCurrencies -> state.value.copy(source = state.value.target, target = state.value.source)
                    else -> State()
                }
            }
        }
    }

    class AmountValidator : IMiddleware<State> {
        override fun process(action: IAction, state: State, processor: IProcessor<State>) {
            if (action is Action.SetAmount) {
                if (action.amount.isEmpty()) {
                    processor.reduce(Action.SetAmount("1"))
                } else {
                    processor.reduce(action)
                }
            }
        }
    }

    class CurrencySelection : IMiddleware<State> {
        override fun process(action: IAction, state: State, processor: IProcessor<State>) {
            if (action is Action.SetSource || action is Action.SetTarget || action is Action.SwitchCurrencies) {
                processor.reduce(action)
            }
        }
    }

    data class State(
        val source: String? = null,
        val target: String? = null,
        val amount: BigDecimal = BigDecimal.ONE,
        val result: BigDecimal? = null
    )

    sealed class Action : IAction {
        data class SetSource(val currency: String) : Action()
        data class SetTarget(val currency: String) : Action()
        data class SetAmount(val amount: String) : Action()
        object SwitchCurrencies: Action()
    }
}

fun String.toBigDecimal() : BigDecimal {
    return BigDecimal(this.replace(".","").replace(',','.'))
}