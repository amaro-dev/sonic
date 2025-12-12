package dev.amaro.sonic

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Transform state and only emit when the selected value changes.
 *
 * Use this in Compose components to prevent unnecessary recompositions.
 * Only recomposes when the selected/transformed data actually changes.
 *
 * Example - Select only notes:
 * ```kotlin
 * @Composable
 * fun NotesList() {
 *     val context = LocalStateManager.current as? StateManagerContext<NoteState> ?: return
 *     val notes by context.manager.listen()
 *         .selectDistinct { it.notes }
 *         .collectAsState()
 *
 *     LazyColumn {
 *         items(notes) { NoteRow(it) }
 *     }
 * }
 * ```
 *
 * Example - Select and transform to display object:
 * ```kotlin
 * data class NoteStats(val total: Int, val open: Int)
 *
 * @Composable
 * fun StatsPanel() {
 *     val context = LocalStateManager.current as? StateManagerContext<NoteState> ?: return
 *     val stats by context.manager.listen()
 *         .selectDistinct { state ->
 *             NoteStats(
 *                 total = state.notes.size,
 *                 open = state.notes.count { !it.done }
 *             )
 *         }
 *         .collectAsState()
 *
 *     Text("${stats.open} open notes")
 * }
 * ```
 *
 * How it works:
 * - Applies the selector function to each emitted state
 * - Uses distinctUntilChanged() to prevent emissions when the selected value equals the previous value
 * - For data classes: structural equality (field-by-field comparison)
 * - For collections: element-by-element comparison
 *
 * Performance: Each selectDistinct() only emits when the transformed value actually changes,
 * allowing Compose to skip recompositions of child components.
 *
 * @param selector Function that transforms full state to selected/derived data
 * @return Flow that only emits when selected value changes (using equals())
 */
fun <T, R> StateFlow<T>.selectDistinct(
    selector: (T) -> R
): Flow<R> = map(selector).distinctUntilChanged()

/**
 * Transform flow values and only emit when the selected value changes.
 * This overload allows chaining selectDistinct() calls.
 *
 * @param selector Function that transforms flow value to selected/derived data
 * @return Flow that only emits when selected value changes (using equals())
 */
fun <T, R> Flow<T>.selectDistinct(
    selector: (T) -> R
): Flow<R> = map(selector).distinctUntilChanged()
