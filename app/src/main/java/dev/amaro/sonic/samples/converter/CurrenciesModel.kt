package dev.amaro.sonic.samples.converter

import androidx.annotation.Size
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface CurrenciesModel {
    fun list(): Flow<CurrencySymbol>
}

class CurrenciesModelImpl: CurrenciesModel {
    override fun list(): Flow<CurrencySymbol> {
        return flowOf(
            CurrencySymbol("MXN"),
            CurrencySymbol("BRL"),
            CurrencySymbol("USD"),
            CurrencySymbol("EUR"),
            CurrencySymbol("GBP"),
            CurrencySymbol("CAD")
        )
    }
}

data class CurrencySymbol(@Size(3) val symbol: String) {
    override fun toString(): String = symbol
}

