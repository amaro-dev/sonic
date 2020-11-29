package dev.amaro.sonic

import dev.amaro.sonic.samples.Calculator
import dev.amaro.sonic.samples.TerminalRenderer

fun main(args: Array<String>) {
    val screen = Calculator.SimpleScreen(TerminalRenderer())
    while (true);
}