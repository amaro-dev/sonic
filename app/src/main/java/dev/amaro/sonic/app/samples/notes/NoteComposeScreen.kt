package dev.amaro.sonic.app.samples.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.amaro.sonic.*
import dev.amaro.sonic.app.compose.LocalStateManager
import dev.amaro.sonic.app.compose.StateManagerContext

/**
 * Compose showcase for selectors + rich result handling.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun MyApp(manager: NoteStateManager) {
 *     CompositionLocalProvider(LocalStateManager provides StateManagerContext(manager)) {
 *         NoteComposeRoot(manager)
 *     }
 * }
 * ```
 */

/**
 * Root composable hosting list/create flows plus state agents.
 */
@Composable
fun NoteComposeRoot(manager: NoteStateManager) {
    LaunchedEffect(manager) {
        manager.perform(Action.Load)
    }
    LaunchedEffect(manager) {
        manager.listen()
            .withAgent(
                ResultClearingAgent(
                    manager,
                    ResultClearingConfig(),
                    { it.result },
                    { Action.ClearResult }
                )
            )
            .collect { }
    }
    var destination by rememberSaveable { mutableStateOf(NoteComposeDestination.LIST) }

    when (destination) {
        NoteComposeDestination.LIST -> NoteComposeScreen(
            onNewNote = {
                manager.perform(Action.NewNote)
                destination = NoteComposeDestination.NEW_NOTE
            },
            onToggleFilter = { manager.perform(Action.ToggleClosedNotes) }
        )

        NoteComposeDestination.NEW_NOTE -> NoteCreateScreen(
            onSave = { title ->
                manager.perform(Action.AddNote(Note(title, false)))
                destination = NoteComposeDestination.LIST
            },
            onCancel = {
                manager.perform(Action.Cancel)
                destination = NoteComposeDestination.LIST
            }
        )
    }
}

private enum class NoteComposeDestination {
    LIST, NEW_NOTE
}

/**
 * Main list screen composable - assumes StateManager provided via CompositionLocal.
 */
@Composable
fun NoteComposeScreen(
    onNewNote: () -> Unit,
    onToggleFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with stats
        NoteHeaderPanel()

        Divider()

        // Main notes list
        NoteListPanel()

        Divider()

        // Action buttons
        NoteActionPanel(onNewNote = onNewNote, onToggleFilter = onToggleFilter)
    }
}

/**
 * Header panel: Shows stats about notes.
 * Only recomposes when the stats change (not on every state update).
 *
 * Demonstrates: selectDistinct() transforming full state to computed stats.
 * If only the notes list changes but counts stay the same, this won't recompose.
 */
@Composable
private fun NoteHeaderPanel() {
    val context = LocalStateManager.current as? StateManagerContext<NoteState> ?: return

    // Select and transform: compute stats from notes
    // Only emits when total or completed count actually changes
    val stats = context.manager.listen()
        .selectDistinct { state ->
            NoteStats(
                total = state.notes.size,
                completed = state.notes.count { it.done },
                open = state.notes.count { !it.done }
            )
        }
        .collectAsState(NoteStats(0, 0, 0))

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Notes",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "${stats.value.completed} of ${stats.value.total} completed • ${stats.value.open} open",
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

/**
 * Notes list panel: Shows all notes based on filter.
 * Only recomposes when the filtered notes list changes.
 *
 * Demonstrates: selectDistinct() filtering without state duplication.
 * Filter logic is applied at render time, not stored in state.
 */
@Composable
private fun NoteListPanel() {
    val context = LocalStateManager.current as? StateManagerContext<NoteState> ?: return

    // Select and filter: only show notes matching the current filter
    // Only emits when the filtered list actually changes
    val displayedNotes = context.manager.listen()
        .selectDistinct { state ->
            if (state.showOnlyOpen) {
                state.notes.filter { !it.done }
            } else {
                state.notes
            }
        }
        .collectAsState(emptyList())

    if (displayedNotes.value.isEmpty()) {
        Text(
            text = "No notes yet. Create one to get started!",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(displayedNotes.value) { note ->
                NoteItemRow(note)
            }
        }
    }
}

/**
 * Individual note row.
 * Each row independently accesses the manager from CompositionLocal.
 */
@Composable
private fun NoteItemRow(note: Note) {
    val context = LocalStateManager.current as? StateManagerContext<NoteState> ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { context.manager.perform(Action.ToggleNote(note)) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Checkbox(
            checked = note.done,
            onCheckedChange = { context.manager.perform(Action.ToggleNote(note)) }
        )
        Text(
            text = note.title,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Action panel: Shows result status and action buttons.
 * Only recomposes when the result status changes.
 *
 * Demonstrates: selectDistinct() with optional state.
 * The full state updates often, but this component only cares about result changes.
 */
@Composable
private fun NoteActionPanel(
    onNewNote: () -> Unit,
    onToggleFilter: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Show result status (success/error) if present
        ResultStatusPanel()

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNewNote,
                modifier = Modifier.weight(1f)
            ) {
                Text("New Note")
            }

            Button(
                onClick = onToggleFilter,
                modifier = Modifier.weight(1f)
            ) {
                Text("Toggle Filter")
            }
        }
    }
}

/**
 * Result status panel: Shows success or error messages.
 * Only recomposes when the result changes.
 */
@Composable
private fun ResultStatusPanel() {
    val context = LocalStateManager.current as? StateManagerContext<NoteState> ?: return

    // Select only the result field
    // Doesn't recompose when notes, filters, or other fields change
    val result = context.manager.listen()
        .selectDistinct { it.result }
        .collectAsState(null)

    val status = result.value ?: return
    when (status) {
        is ResultInfo.Success -> {
            val operation = status.metadata["operation"] ?: "success"
            Text(
                text = "✓ ${operation.toString().replace('_', ' ')} via ${status.source}",
                fontSize = 12.sp
            )
        }

        is ResultInfo.Failure -> {
            Text(
                text = "✗ ${status.code}: ${status.message}",
                fontSize = 12.sp
            )
            if (status.retryable) {
                Text(
                    text = "Retry available",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

/**
 * Value class for computed stats.
 * Used by NoteHeaderPanel to prevent unnecessary recompositions.
 * Only emits new values when total, completed, or open count actually changes.
 */
data class NoteStats(
    val total: Int,
    val completed: Int,
    val open: Int
)

/**
 * Compose screen for creating a new note.
 */
@Composable
private fun NoteCreateScreen(
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Create Note", fontSize = 24.sp)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") }
        )
        ResultStatusPanel()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title)
                        title = ""
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
            Button(
                onClick = {
                    title = ""
                    onCancel()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
        }
    }
}
