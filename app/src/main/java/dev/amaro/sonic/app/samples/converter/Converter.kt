package dev.amaro.sonic.app.samples.converter

import dev.amaro.sonic.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.math.BigDecimal
import java.math.RoundingMode

object Converter {

    class Screen(
        renderer: IRenderer<State>,
        initialState: State = State(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    ) {
        private val manager = SimpleStateManager(initialState, scope)

        init {
            bindState(manager, renderer, scope)
        }

        fun perform(action: IAction) {
            manager.perform(action)
        }
    }

    class SimpleStateManager(
        initialState: State,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    ) :
        StateManager<State>(initialState, scope) {
        init {
            addMiddleware(AmountValidator())
            addMiddleware(CurrencySelection())
            addMiddleware(ConversionCalculator())
        }

        override val reducer: IReducer<State> = object : IReducer<State> {
            override fun reduce(action: IAction, currentState: Converter.State): Converter.State {
                return when (action) {
                    is Action.SetSource -> currentState.copy(source = action.currency)
                    is Action.SetTarget -> currentState.copy(target = action.currency)
                    is Action.SetAmount -> currentState.copy(amount = action.amount.toBigDecimal())
                    is Action.SetResult -> currentState.copy(
                        result = action.amount?.setScale(
                            2,
                            RoundingMode.HALF_UP
                        )
                    )
                    is Action.SetStatus -> currentState.copy(status = action.status)
                    is Action.SwitchCurrencies -> currentState.copy(
                        source = state.value.target,
                        target = state.value.source
                    )
                    else -> currentState
                }
            }
        }
    }

    class AmountValidator : IMiddleware<State> {
        override suspend fun process(action: IAction, state: State, processor: IProcessor<State>) {
            if (action is Action.SetAmount) {
                if (action.amount.isEmpty()) {
                    processor.reduce(Action.SetAmount("1"))
                } else {
                    processor.reduce(action)
                }
                processor.perform(Action.Refresh)
            }
        }
    }

    class CurrencySelection : IMiddleware<State> {
        override suspend fun process(action: IAction, state: State, processor: IProcessor<State>) {
            if (action is Action.SetSource || action is Action.SetTarget || action is Action.SwitchCurrencies) {
                processor.reduce(action)
                processor.perform(Action.Refresh)
            }
        }
    }

    class ConversionCalculator : IMiddleware<State> {
        private val rates = mapOf(
            Pair(Pair("MXN", "BRL"), BigDecimal("0.26568")),
            Pair(Pair("MXN", "USD"), BigDecimal("0.04974")),
            Pair(Pair("MXN", "EUR"), BigDecimal("0.04157")),
            Pair(Pair("MXN", "GBP"), BigDecimal("0.03729")),
            Pair(Pair("MXN", "CAD"), BigDecimal("0.06455")),

            Pair(Pair("BRL", "MXN"), BigDecimal("3.75970")),
            Pair(Pair("BRL", "USD"), BigDecimal("0.18709")),
            Pair(Pair("BRL", "EUR"), BigDecimal("0.15635")),
            Pair(Pair("BRL", "GBP"), BigDecimal("0.14024")),
            Pair(Pair("BRL", "CAD"), BigDecimal("0.24277")),

            Pair(Pair("USD", "MXN"), BigDecimal("20.0962")),
            Pair(Pair("USD", "BRL"), BigDecimal("5.34116")),
            Pair(Pair("USD", "EUR"), BigDecimal("0.83573")),
            Pair(Pair("USD", "GBP"), BigDecimal("0.74958")),
            Pair(Pair("USD", "CAD"), BigDecimal("1.29766")),

            Pair(Pair("EUR", "MXN"), BigDecimal("24.0436")),
            Pair(Pair("EUR", "BRL"), BigDecimal("6.39029")),
            Pair(Pair("EUR", "USD"), BigDecimal("1.19642")),
            Pair(Pair("EUR", "GBP"), BigDecimal("0.89684")),
            Pair(Pair("EUR", "CAD"), BigDecimal("1.55256")),

            Pair(Pair("GBP", "MXN"), BigDecimal("26.8061")),
            Pair(Pair("GBP", "BRL"), BigDecimal("7.12452")),
            Pair(Pair("GBP", "USD"), BigDecimal("1.33389")),
            Pair(Pair("GBP", "EUR"), BigDecimal("1.11479")),
            Pair(Pair("GBP", "CAD"), BigDecimal("1.73094")),

            Pair(Pair("CAD", "MXN"), BigDecimal("15.4843")),
            Pair(Pair("CAD", "BRL"), BigDecimal("4.11540")),
            Pair(Pair("CAD", "USD"), BigDecimal("0.77051")),
            Pair(Pair("CAD", "EUR"), BigDecimal("0.64393")),
            Pair(Pair("CAD", "GBP"), BigDecimal("0.57756"))
        )

        override suspend fun process(action: IAction, state: State, processor: IProcessor<State>) {
            if (state.source != null && state.target != null) {
                val result = rates[Pair(state.source, state.target)]?.multiply(state.amount)
                processor.reduce(Action.SetResult(result))
            }
        }
    }

    data class State(
        val source: String? = null,
        val target: String? = null,
        val amount: BigDecimal = BigDecimal.ONE,
        val result: BigDecimal? = null,
        val status: Status? = null
    ) {
        private val options: Set<CurrencySymbol?> = setOf(
            null,
            CurrencySymbol("MXN"),
            CurrencySymbol("BRL"),
            CurrencySymbol("USD"),
            CurrencySymbol("EUR"),
            CurrencySymbol("GBP"),
            CurrencySymbol("CAD")
        )
        val sourceOptions: List<CurrencySymbol?>
            get() = options.filter { it?.symbol != target ?: true }.toList()
        val targetOptions: List<CurrencySymbol?>
            get() = options.filter { it?.symbol != source ?: true }.toList()
    }

    sealed class Action : IAction {
        data class SetSource(val currency: String) : Action()
        data class SetTarget(val currency: String) : Action()
        data class SetAmount(val amount: String) : Action()
        data class SetResult(val amount: BigDecimal?) : Action()
        data class SetStatus(val status: Status) : Action()
        object Refresh : Action()
        object SwitchCurrencies : Action()
    }
}

fun String.toBigDecimal(): BigDecimal {
    return BigDecimal(this.replace(".", "").replace(',', '.'))
}
