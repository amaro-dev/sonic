package dev.amaro.sonic.app.samples.notes

data class NoteState(
    val notes: List<Note> = emptyList(),
    val showOnlyOpen: Boolean = false
)