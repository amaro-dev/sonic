package dev.amaro.sonic.app.samples.notes

import dev.amaro.sonic.IStatusContainer
import dev.amaro.sonic.ResultInfo

data class NoteState(
    val notes: List<Note> = emptyList(),
    val showOnlyOpen: Boolean = false,
    val result: ResultInfo? = null
) : IStatusContainer {
    override val resultInfo: ResultInfo?
        get() = result
}
