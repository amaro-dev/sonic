package dev.amaro.sonic.app.samples.notes

import dev.amaro.sonic.IStatusContainer
import dev.amaro.sonic.Status

data class NoteState(
    val notes: List<Note> = emptyList(),
    val showOnlyOpen: Boolean = false,
    override val status: Status? = null
) : IStatusContainer
