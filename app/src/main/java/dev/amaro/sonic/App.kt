package dev.amaro.sonic

import dev.amaro.sonic.samples.calculator.Calculator
import dev.amaro.sonic.samples.calculator.TerminalRenderer

fun main(args: Array<String>) {
    val screen = Calculator.SimpleScreen(TerminalRenderer())
    while (true);
}