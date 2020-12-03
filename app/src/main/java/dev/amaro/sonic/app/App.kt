package dev.amaro.sonic.app

import dev.amaro.sonic.app.samples.calculator.Calculator
import dev.amaro.sonic.app.samples.calculator.TerminalRenderer

fun main(args: Array<String>) {
    val screen = Calculator.SimpleScreen(TerminalRenderer())
    while (true);
}